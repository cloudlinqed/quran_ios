# Quran Recitation Review System

A real-time Quran recitation evaluation system that uses AI-powered speech recognition to assess recitation accuracy.

## Overview

This system allows users to recite Quran verses and receive instant feedback on their accuracy. It uses a two-stage pipeline: audio segmentation followed by speech recognition and text alignment.

## Architecture

```
┌─────────────────┐     WebSocket      ┌──────────────────────────────────────┐
│   Android App   │ ◄───────────────► │         Python Server                │
│                 │   Audio Stream     │                                      │
│ - Record Audio  │                    │  ┌─────────────────────────────────┐ │
│ - Select Verse  │                    │  │ Stage 1: Segmentation           │ │
│ - Show Results  │   JSON Response    │  │ (recitation-segmenter-v2)       │ │
│                 │ ◄──────────────── │  └─────────────┬───────────────────┘ │
└─────────────────┘                    │                │                      │
                                       │                ▼                      │
                                       │  ┌─────────────────────────────────┐ │
                                       │  │ Stage 2: ASR + Alignment        │ │
                                       │  │ (wav2vec2-xlsr-53-arabic-quran) │ │
                                       │  └─────────────────────────────────┘ │
                                       └──────────────────────────────────────┘
```

## Models Used

### 1. Recitation Segmenter

| Property | Value |
|----------|-------|
| **Model** | `obadx/recitation-segmenter-v2` |
| **URL** | https://huggingface.co/obadx/recitation-segmenter-v2 |
| **Architecture** | Wav2Vec2Bert (fine-tuned from facebook/w2v-bert-2.0) |
| **Purpose** | Segments Quran recitation audio based on pause points (waqf) |
| **Accuracy** | 99.58% on evaluation set |
| **Resolution** | 20ms frame-level classification |
| **Memory** | ~3GB GPU |

**Key Features:**
- Detects natural pause points (waqf) in recitation
- Handles any audio duration without degradation
- High precision segmentation for phrase-level analysis

### 2. Arabic Quran ASR

| Property | Value |
|----------|-------|
| **Model** | `rabah2026/wav2vec2-large-xlsr-53-arabic-quran-v_final` |
| **URL** | https://huggingface.co/rabah2026/wav2vec2-large-xlsr-53-arabic-quran-v_final |
| **Architecture** | Wav2Vec2 (fine-tuned from facebook/wav2vec2-large-xlsr-53) |
| **Purpose** | Transcribes Arabic Quran recitation to text |
| **Training Data** | Quranic recitation audio |
| **Sample Rate** | 16kHz |

**Key Features:**
- Specifically trained on Quran recitations
- Understands Tajweed rules and pronunciation
- Outputs Arabic text directly

## Methodology

### Pipeline Flow

```
Audio Input
    │
    ▼
┌───────────────────────────────────────┐
│ 1. SEGMENTATION                       │
│    - Input: Raw audio (16kHz PCM)     │
│    - Model: recitation-segmenter-v2   │
│    - Output: Audio segments with      │
│              timestamps (waqf points) │
└───────────────────────────────────────┘
    │
    ▼
┌───────────────────────────────────────┐
│ 2. TRANSCRIPTION                      │
│    - Input: Each audio segment        │
│    - Model: wav2vec2-xlsr-53-quran    │
│    - Output: Arabic text per segment  │
└───────────────────────────────────────┘
    │
    ▼
┌───────────────────────────────────────┐
│ 3. ALIGNMENT & SCORING                │
│    - Compare transcription vs         │
│      expected Quran text              │
│    - Word-by-word alignment using     │
│      Levenshtein distance             │
│    - Calculate similarity scores      │
└───────────────────────────────────────┘
    │
    ▼
┌───────────────────────────────────────┐
│ 4. DECISION                           │
│    - Pass threshold: 70%              │
│    - Output: Pass/Fail + word report  │
└───────────────────────────────────────┘
```

### Scoring Algorithm

1. **Text Normalization**: Remove extra whitespace, normalize text
2. **Word Alignment**: Dynamic programming (edit distance) to align expected and transcribed words
3. **Similarity Calculation**: Levenshtein ratio between word pairs
4. **Classification**:
   - `correct`: similarity ≥ 70%
   - `wrong`: similarity < 70%
   - `missing`: word in expected but not transcribed
   - `extra`: word in transcribed but not expected
5. **Overall Accuracy**: Full text similarity score
6. **Pass/Fail**: Accuracy ≥ 70% threshold

### Word Match Types

| Type | Description | Color (UI) |
|------|-------------|------------|
| `correct` | Word matches expected (≥70% similarity) | Green |
| `wrong` | Word differs from expected (<70% similarity) | Yellow |
| `missing` | Expected word not found in transcription | Red |
| `extra` | Transcribed word not in expected text | Gray |

## Data Sources

### Quran Text

| File | Source | Description |
|------|--------|-------------|
| `quran_uthmani_min.json` | AlQuran Cloud API | Uthmani script with simple tashkeel |

**Edition**: `quran-uthmani-min` - Standard Arabic diacritics without special Tajweed Unicode marks for better font compatibility.

**API**: https://api.alquran.cloud/v1/quran/quran-uthmani-min

## API Response Format

```json
{
  "status": "complete",
  "expected_text": "بِسمِ اللَّهِ الرَّحمٰنِ الرَّحيمِ",
  "transcribed_text": "بسم الله الرحمن الرحيم",
  "accuracy": 85.5,
  "passed": true,
  "word_matches": [
    {
      "expected": "بِسمِ",
      "transcribed": "بسم",
      "type": "correct",
      "expected_index": 0,
      "transcribed_index": 0,
      "similarity": 80.0
    }
  ],
  "segments": [
    {
      "index": 0,
      "start_time": 0.0,
      "end_time": 2.5,
      "expected": "بِسمِ اللَّهِ",
      "transcribed": "بسم الله",
      "similarity": 82.0,
      "is_correct": true
    }
  ]
}
```

## System Requirements

### Server
- Python 3.10+
- CUDA-capable GPU (recommended, ~3GB VRAM)
- 8GB+ RAM

### Dependencies
```
fastapi>=0.109.0
uvicorn[standard]>=0.27.0
transformers>=4.51.0
torch>=2.6.0
torchaudio>=2.6.0
recitations-segmenter
python-Levenshtein>=0.23.0
```

### Android Client
- Min SDK: 26 (Android 8.0)
- Jetpack Compose
- WebSocket for real-time streaming

## File Structure

```
textquran/
├── server/
│   ├── main.py                 # FastAPI server with WebSocket
│   ├── model_loader.py         # ASR model loading
│   ├── segmenter.py            # Audio segmentation
│   ├── alignment_scorer.py     # Scoring and alignment
│   ├── transcriber.py          # Audio transcription
│   ├── quran_data.py           # Quran text loading
│   └── quran_uthmani_min.json  # Quran text data
│
├── android/
│   └── app/src/main/
│       ├── java/com/quran/reciter/
│       │   ├── ui/             # Compose UI screens
│       │   ├── viewmodel/      # ViewModel
│       │   ├── audio/          # Audio recording
│       │   ├── network/        # WebSocket client
│       │   └── data/           # Data models
│       └── assets/
│           └── quran_display.json
│
└── SOLUTION.md                 # This documentation
```

## Running the System

### Server
```bash
cd server
pip install -r requirements.txt
python main.py
```

Server starts at `http://0.0.0.0:8000`

### Android
1. Open `android/` folder in Android Studio
2. Update `SERVER_URL` in `Models.kt` to your server IP
3. Build and run on device

## Limitations

1. **Segmenter** treats short pauses (sakt) as full stops (waqf)
2. **ASR Model** optimized for standard Quran recitation styles
3. **Text Matching** uses character-level comparison, may not capture all Tajweed nuances
4. **GPU Required** for optimal performance (CPU fallback available but slower)

## Code Snippets

### Loading the Segmenter Model

```python
from transformers import AutoFeatureExtractor, AutoModelForAudioFrameClassification
import torch

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
dtype = torch.bfloat16 if torch.cuda.is_available() else torch.float32

processor = AutoFeatureExtractor.from_pretrained("obadx/recitation-segmenter-v2")
model = AutoModelForAudioFrameClassification.from_pretrained("obadx/recitation-segmenter-v2")
model.to(device, dtype=dtype)
model.eval()
```

### Segmenting Audio

```python
from recitations_segmenter import segment_recitations, clean_speech_intervals

# Segment the audio (audio should be 16kHz numpy array)
outputs = segment_recitations(
    [audio],
    model,
    processor,
    device=device,
    dtype=dtype,
    batch_size=1,
)

# Clean the speech intervals
clean_result = clean_speech_intervals(
    outputs[0].speech_intervals,
    outputs[0].is_complete,
    min_silence_duration_ms=50,
    min_speech_duration_ms=100,
    pad_duration_ms=30,
    return_seconds=True,
)

# Extract segments
for interval in clean_result.clean_speech_intervals:
    start_time, end_time = interval[0], interval[1]
    start_sample = int(start_time * 16000)
    end_sample = int(end_time * 16000)
    segment_audio = audio[start_sample:end_sample]
```

### Loading the ASR Model

```python
from transformers import Wav2Vec2ForCTC, Wav2Vec2Processor
import torch

model_name = "rabah2026/wav2vec2-large-xlsr-53-arabic-quran-v_final"
device = "cuda" if torch.cuda.is_available() else "cpu"

processor = Wav2Vec2Processor.from_pretrained(model_name)
model = Wav2Vec2ForCTC.from_pretrained(model_name)
model.to(device)
model.eval()
```

### Transcribing Audio

```python
def transcribe(audio_array, sample_rate=16000):
    inputs = processor(
        audio_array,
        sampling_rate=sample_rate,
        return_tensors="pt"
    )
    input_values = inputs.input_values.to(device)

    with torch.no_grad():
        logits = model(input_values).logits

    predicted_ids = torch.argmax(logits, dim=-1)
    transcription = processor.decode(predicted_ids[0])

    return transcription
```

### Word Alignment (Levenshtein-based)

```python
import Levenshtein

def calculate_similarity(word1, word2):
    if not word1 or not word2:
        return 0.0
    if word1 == word2:
        return 1.0
    distance = Levenshtein.distance(word1, word2)
    max_len = max(len(word1), len(word2))
    return 1.0 - (distance / max_len)

def align_words(expected_words, transcribed_words, threshold=0.7):
    m, n = len(expected_words), len(transcribed_words)

    # DP table for edit distance
    dp = [[0] * (n + 1) for _ in range(m + 1)]
    for i in range(m + 1):
        dp[i][0] = i
    for j in range(n + 1):
        dp[0][j] = j

    for i in range(1, m + 1):
        for j in range(1, n + 1):
            sim = calculate_similarity(expected_words[i-1], transcribed_words[j-1])
            cost = 0 if sim >= threshold else 1
            dp[i][j] = min(
                dp[i-1][j] + 1,      # deletion (missing)
                dp[i][j-1] + 1,      # insertion (extra)
                dp[i-1][j-1] + cost  # substitution or match
            )

    # Backtrack to get alignment
    matches = []
    i, j = m, n
    while i > 0 or j > 0:
        if i > 0 and j > 0:
            sim = calculate_similarity(expected_words[i-1], transcribed_words[j-1])
            cost = 0 if sim >= threshold else 1
            if dp[i][j] == dp[i-1][j-1] + cost:
                match_type = "correct" if sim >= threshold else "wrong"
                matches.append({
                    "expected": expected_words[i-1],
                    "transcribed": transcribed_words[j-1],
                    "type": match_type,
                    "similarity": sim
                })
                i -= 1
                j -= 1
            elif dp[i][j] == dp[i-1][j] + 1:
                matches.append({"expected": expected_words[i-1], "transcribed": "", "type": "missing"})
                i -= 1
            else:
                matches.append({"expected": "", "transcribed": transcribed_words[j-1], "type": "extra"})
                j -= 1
        elif i > 0:
            matches.append({"expected": expected_words[i-1], "transcribed": "", "type": "missing"})
            i -= 1
        else:
            matches.append({"expected": "", "transcribed": transcribed_words[j-1], "type": "extra"})
            j -= 1

    matches.reverse()
    return matches
```

### WebSocket Handler (Server)

```python
@app.websocket("/ws/transcribe")
async def websocket_transcribe(websocket: WebSocket):
    await websocket.accept()
    transcriber = AudioTranscriber()

    try:
        while True:
            data = await websocket.receive()

            if "text" in data:
                message = json.loads(data["text"])
                action = message.get("action")

                if action == "start":
                    # Initialize session with surah/verse info
                    expected_text = quran_data.get_verses_text_range(
                        message["surah_id"],
                        message["verse_start"],
                        message["verse_end"]
                    )
                    transcriber.clear_buffer()
                    await websocket.send_json({"status": "ready", "expected_text": expected_text})

                elif action == "stop":
                    # Process full audio
                    audio = transcriber.get_buffered_audio()

                    # Segment -> Transcribe -> Score
                    segments = segmenter.segment(audio)
                    result = alignment_scorer.score_segments(segments.segments, expected_text)

                    await websocket.send_json({
                        "status": "complete",
                        **alignment_scorer.to_dict(result)
                    })

            elif "bytes" in data:
                # Receive audio chunks
                transcriber.process_audio_chunk(data["bytes"])

    except WebSocketDisconnect:
        print("Client disconnected")
```

### Android WebSocket Client

```kotlin
class WebSocketClient(private val serverUrl: String) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    fun connect(): Flow<WebSocketEvent> = callbackFlow {
        val request = Request.Builder().url("$serverUrl/ws/transcribe").build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val result = Json.decodeFromString<RecitationResult>(text)
                trySend(WebSocketEvent.Message(result))
            }
        })

        awaitClose { webSocket?.close(1000, "Closing") }
    }

    fun startSession(surahId: Int, verseStart: Int, verseEnd: Int) {
        val message = """{"action":"start","surah_id":$surahId,"verse_start":$verseStart,"verse_end":$verseEnd}"""
        webSocket?.send(message)
    }

    fun sendAudioChunk(chunk: ByteArray) {
        webSocket?.send(chunk.toByteString())
    }

    fun stopSession() {
        webSocket?.send("""{"action":"stop"}""")
    }
}
```

### Android Audio Recording

```kotlin
class AudioRecorder(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private val sampleRate = 16000
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    fun startRecording(): Flow<ByteArray> = flow {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        audioRecord?.startRecording()
        val buffer = ByteArray(bufferSize)

        while (currentCoroutineContext().isActive) {
            val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
            if (read > 0) {
                emit(buffer.copyOf(read))
            }
        }
    }.flowOn(Dispatchers.IO)

    fun stopRecording() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
```

## Future Improvements

- [ ] Fine-tune matching for connected words (wasl)
- [ ] Add support for different recitation styles (Hafs, Warsh, etc.)
- [ ] Implement phonetic similarity for better Arabic matching
- [ ] Add verse auto-detection from recitation
