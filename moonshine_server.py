#!/usr/bin/env python3
"""
Moonshine Tiny AR Server for Quran Transcription

A simple HTTP server that hosts the Moonshine Tiny AR model for real-time
Arabic transcription. Designed for POC testing with the Android app.

Usage:
    pip install torch torchaudio transformers librosa soundfile flask flask-cors
    python moonshine_server.py

API:
    POST /transcribe
        - Body: Raw PCM16 audio bytes (24kHz mono)
        - Returns: JSON { "transcript": "..." }

    GET /health
        - Returns: JSON { "status": "ok", "model": "moonshine-tiny-ar" }
"""

import io
import torch
import librosa
import numpy as np
from flask import Flask, request, jsonify
from flask_cors import CORS
from transformers import MoonshineForConditionalGeneration, AutoProcessor
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Flask app
app = Flask(__name__)
CORS(app)

# Model configuration
MODEL_NAME = "UsefulSensors/moonshine-tiny-ar"
SAMPLE_RATE = 16000  # Moonshine expects 16kHz

# Global model and processor
model = None
processor = None
device = None
torch_dtype = None


def load_model():
    """Load the Moonshine Tiny AR model."""
    global model, processor, device, torch_dtype

    logger.info(f"Loading model: {MODEL_NAME}")

    # Determine device and dtype
    device = "cuda:0" if torch.cuda.is_available() else "cpu"
    torch_dtype = torch.float16 if torch.cuda.is_available() else torch.float32

    logger.info(f"Using device: {device}, dtype: {torch_dtype}")

    # Load processor and model
    processor = AutoProcessor.from_pretrained(MODEL_NAME)
    model = MoonshineForConditionalGeneration.from_pretrained(MODEL_NAME)
    model = model.to(device).to(torch_dtype)

    logger.info("Model loaded successfully")


def transcribe_audio(audio_bytes: bytes, input_sample_rate: int = 24000) -> str:
    """
    Transcribe audio bytes to Arabic text.

    Args:
        audio_bytes: Raw PCM16 audio bytes
        input_sample_rate: Sample rate of input audio (default 24kHz from Android)

    Returns:
        Transcribed Arabic text
    """
    if model is None or processor is None:
        raise RuntimeError("Model not loaded")

    # Convert bytes to numpy array (PCM16 little-endian)
    audio_np = np.frombuffer(audio_bytes, dtype=np.int16).astype(np.float32) / 32768.0

    # Resample from input rate to model's expected rate (16kHz)
    if input_sample_rate != SAMPLE_RATE:
        audio_np = librosa.resample(
            audio_np,
            orig_sr=input_sample_rate,
            target_sr=SAMPLE_RATE
        )

    # Skip if audio is too short (less than 100ms)
    min_samples = int(SAMPLE_RATE * 0.1)
    if len(audio_np) < min_samples:
        logger.warning(f"Audio too short: {len(audio_np)} samples")
        return ""

    # Process audio
    inputs = processor(audio_np, return_tensors="pt", sampling_rate=SAMPLE_RATE)
    inputs = inputs.to(device, torch_dtype)

    # Calculate max_length to prevent hallucination
    # Based on model documentation: ~13 tokens per second of audio
    token_limit_factor = 13 / SAMPLE_RATE
    seq_lens = inputs.attention_mask.sum(dim=-1)
    max_length = int((seq_lens * token_limit_factor).max().item())

    # Ensure minimum max_length
    max_length = max(max_length, 10)

    logger.debug(f"Audio length: {len(audio_np) / SAMPLE_RATE:.2f}s, max_length: {max_length}")

    # Generate transcription
    with torch.no_grad():
        generated_ids = model.generate(**inputs, max_length=max_length)

    # Decode
    transcript = processor.decode(generated_ids[0], skip_special_tokens=True)

    return transcript.strip()


@app.route('/health', methods=['GET'])
def health():
    """Health check endpoint."""
    return jsonify({
        "status": "ok",
        "model": MODEL_NAME,
        "device": str(device),
        "ready": model is not None
    })


@app.route('/transcribe', methods=['POST'])
def transcribe():
    """
    Transcribe audio endpoint.

    Expects:
        - Body: Raw PCM16 audio bytes
        - Header: X-Sample-Rate (optional, default 24000)

    Returns:
        JSON { "transcript": "...", "success": true }
    """
    try:
        # Get audio data
        audio_bytes = request.data
        if not audio_bytes:
            return jsonify({"success": False, "error": "No audio data"}), 400

        # Get sample rate from header (Android sends 24kHz)
        sample_rate = int(request.headers.get('X-Sample-Rate', 24000))

        logger.info(f"Received {len(audio_bytes)} bytes at {sample_rate}Hz")

        # Transcribe
        transcript = transcribe_audio(audio_bytes, sample_rate)

        logger.info(f"Transcription: {transcript}")

        return jsonify({
            "success": True,
            "transcript": transcript
        })

    except Exception as e:
        logger.error(f"Transcription error: {e}", exc_info=True)
        return jsonify({
            "success": False,
            "error": str(e)
        }), 500


if __name__ == '__main__':
    # Load model at startup
    load_model()

    # Run server
    # Use 0.0.0.0 to allow connections from Android device
    print("\n" + "="*60)
    print("Moonshine Tiny AR Server")
    print("="*60)
    print(f"Model: {MODEL_NAME}")
    print(f"Device: {device}")
    print(f"Endpoints:")
    print(f"  - GET  /health     - Check server status")
    print(f"  - POST /transcribe - Transcribe audio")
    print("="*60 + "\n")

    app.run(host='0.0.0.0', port=5000, debug=False)
