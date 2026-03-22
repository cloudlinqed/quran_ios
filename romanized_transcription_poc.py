"""
POC: Romanized Quran Transcription Matching

Approach:
1. User recites Arabic Quran
2. Transcribe using English ASR (produces phonetic romanization)
3. Match against known transliterations from quran_words_all.json
4. Use forced alignment for word-level timestamps

Benefits:
- English ASR is more robust than Arabic ASR
- Romanized text matching is simpler than Arabic diacritics
- Word boundaries are cleaner
- Can leverage existing forced alignment tools
"""

import json
import os
import sys
from difflib import SequenceMatcher
import re

# Fix Windows console encoding
if sys.platform == 'win32':
    sys.stdout.reconfigure(encoding='utf-8')

# Load Surah 111 data
def load_surah_111():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    json_path = os.path.join(script_dir, "quran_data", "quran-md", "surah_111.json")

    with open(json_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    return data

def normalize_transliteration(text):
    """Normalize transliteration for comparison."""
    # Remove diacritical marks and special characters
    text = text.lower()
    # Replace common variants
    replacements = {
        'ā': 'a', 'ī': 'i', 'ū': 'u',
        'ṣ': 's', 'ḍ': 'd', 'ṭ': 't', 'ẓ': 'z',
        'ḥ': 'h', 'ġ': 'gh', 'ḫ': 'kh',
        'ʿ': '', 'ʾ': '', "'": "",
        '-': '', '_': ''
    }
    for old, new in replacements.items():
        text = text.replace(old, new)
    return text


def filter_latin_words(text):
    """Filter out non-Latin garbage from transcription."""
    words = re.split(r'\s+', text.strip())
    filtered = []
    for word in words:
        # Remove punctuation for checking
        clean = re.sub(r'[^\w]', '', word)
        if not clean:
            continue
        # Keep only words that are primarily ASCII/Latin
        ascii_chars = sum(1 for c in clean if c.isascii() and c.isalpha())
        if len(clean) > 0 and ascii_chars / len(clean) >= 0.8:
            # Skip very short words (likely English articles/prepositions)
            if len(clean) >= 2:
                filtered.append(word)
    return filtered


def is_likely_english_garbage(word):
    """Check if word is likely English garbage, not Arabic romanization."""
    common_english = {
        'the', 'a', 'an', 'in', 'on', 'at', 'to', 'for', 'of', 'and', 'or',
        'is', 'it', 'be', 'as', 'by', 'was', 'are', 'been', 'being',
        'have', 'has', 'had', 'do', 'does', 'did', 'will', 'would', 'could',
        'should', 'may', 'might', 'must', 'shall', 'can', 'need', 'dare',
        'i', 'you', 'he', 'she', 'we', 'they', 'me', 'him', 'her', 'us', 'them',
        'my', 'your', 'his', 'its', 'our', 'their', 'this', 'that', 'these', 'those',
        'what', 'which', 'who', 'whom', 'whose', 'where', 'when', 'why', 'how',
        'all', 'each', 'every', 'both', 'few', 'more', 'most', 'other', 'some',
        'such', 'no', 'not', 'only', 'own', 'same', 'so', 'than', 'too', 'very',
        'just', 'but', 'if', 'because', 'until', 'while', 'with', 'about',
        'against', 'between', 'into', 'through', 'during', 'before', 'after',
        'above', 'below', 'from', 'up', 'down', 'out', 'off', 'over', 'under',
        'again', 'further', 'then', 'once', 'here', 'there', 'any', 'pray',
        'leadership', 'umbled', 'humble', 'speech', 'orange', 'please', 'thank'
    }
    clean = re.sub(r'[^\w]', '', word.lower())
    return clean in common_english

def get_all_transliterations(surah_data):
    """Extract all transliterations from surah data."""
    words = []
    for ayah_num, ayah_data in surah_data['ayahs'].items():
        for word in ayah_data['words']:
            words.append({
                'ayah': int(ayah_num),
                'index': word['index'],
                'arabic': word['arabic'],
                'transliteration': word['transliteration'],
                'normalized': normalize_transliteration(word['transliteration'])
            })
    return words

def fuzzy_match(transcribed_word, expected_words, threshold=0.6):
    """Find best matching word from expected words."""
    normalized_transcribed = normalize_transliteration(transcribed_word)

    best_match = None
    best_score = 0

    for word in expected_words:
        score = SequenceMatcher(None, normalized_transcribed, word['normalized']).ratio()
        if score > best_score and score >= threshold:
            best_score = score
            best_match = word

    return best_match, best_score

def match_transcription(transcription, expected_words, threshold=0.5):
    """Match transcribed words against expected transliterations."""
    # Split into words
    transcribed_words = transcription.strip().split()

    results = []
    matched_indices = set()

    for trans_word in transcribed_words:
        if not trans_word:
            continue

        # Filter out already matched words for sequential matching
        available_words = [w for i, w in enumerate(expected_words)
                          if i not in matched_indices]

        match, score = fuzzy_match(trans_word, available_words, threshold)

        if match:
            idx = expected_words.index(match)
            matched_indices.add(idx)
            results.append({
                'transcribed': trans_word,
                'matched_arabic': match['arabic'],
                'matched_translit': match['transliteration'],
                'ayah': match['ayah'],
                'word_index': match['index'],
                'score': score,
                'status': 'MATCH' if score >= 0.7 else 'FUZZY'
            })
        else:
            # Only show NO_MATCH if it looks like an Arabic romanization attempt
            # (not just random English)
            results.append({
                'transcribed': trans_word,
                'matched_arabic': None,
                'matched_translit': None,
                'ayah': None,
                'word_index': None,
                'score': 0,
                'status': 'NO_MATCH'
            })

    return results

def test_with_simulated_transcription():
    """Test with simulated English ASR output for Surah 111."""
    print("=" * 60)
    print("POC: Romanized Quran Transcription Matching")
    print("=" * 60)

    # Load Surah 111
    surah = load_surah_111()
    expected_words = get_all_transliterations(surah)

    print(f"\nSurah: {surah['surah_name_ar']} ({surah['surah_name_en']})")
    print(f"Total words: {len(expected_words)}")
    print("\nExpected transliterations:")
    for i, word in enumerate(expected_words):
        print(f"  {i+1}. [{word['ayah']}:{word['index']}] {word['arabic']} → {word['transliteration']}")

    # Simulated English ASR outputs for Arabic recitation
    # These simulate what an English model might hear when someone recites Arabic
    test_cases = [
        # Good recitation (close to transliterations)
        "tabbat yada abi lahabin watabba ma aghna anhu maluhu wama kasab",

        # With some pronunciation variations
        "tabbat yeda abi lahab wa tabba ma aghna 'anhu maalohu wa ma kasab",

        # Partial recitation (first ayah only)
        "tabbat yada abi lahabin watabba",

        # With English ASR artifacts (how it might actually hear Arabic)
        "tabbet yeda abee lahab weh taba",
    ]

    print("\n" + "=" * 60)
    print("Testing with simulated transcriptions:")
    print("=" * 60)

    for i, transcription in enumerate(test_cases):
        print(f"\n--- Test {i+1} ---")
        print(f"Transcription: '{transcription}'")

        results = match_transcription(transcription, expected_words)

        matches = sum(1 for r in results if r['status'] in ['MATCH', 'FUZZY'])
        print(f"Matched: {matches}/{len(results)} words")

        for r in results:
            if r['status'] == 'MATCH':
                print(f"  ✅ '{r['transcribed']}' → {r['matched_arabic']} ({r['score']:.0%})")
            elif r['status'] == 'FUZZY':
                print(f"  🟡 '{r['transcribed']}' ~ {r['matched_arabic']} ({r['score']:.0%})")
            else:
                print(f"  ❌ '{r['transcribed']}' → NO MATCH")

def load_api_key():
    """Load OpenAI API key from environment or keys.txt"""
    api_key = os.environ.get('OPENAI_API_KEY')
    if not api_key:
        keys_path = os.path.join(os.path.dirname(__file__), 'keys.txt')
        if os.path.exists(keys_path):
            with open(keys_path, 'r') as f:
                for line in f:
                    if 'OPENAI' in line.upper() and '=' in line:
                        api_key = line.split('=')[1].strip()
                        break
    return api_key


def test_openai_whisper_transcription(audio_path):
    """
    Test with actual OpenAI Whisper API.

    Usage:
        python romanized_transcription_poc.py path/to/arabic_audio.wav
    """
    try:
        from openai import OpenAI
    except ImportError:
        print("OpenAI library not installed. Run: pip install openai")
        return

    api_key = load_api_key()
    if not api_key:
        print("OPENAI_API_KEY not found")
        return

    client = OpenAI(api_key=api_key)

    print(f"\nTranscribing: {audio_path}")

    # Transcribe with Whisper - use prompt to force phonetic transcription
    # The prompt guides Whisper to output romanized Arabic words
    phonetic_prompt = """Transcribe the Arabic speech phonetically using Latin letters.
Example words: tabbat, yada, abi, lahabin, watabba, ma, aghna, anhu, maluhu, kasaba,
sayasla, naran, dhata, waimraatuhu, hammalata, alhatabi, fi, jidiha, hablun, min, masadin.
Do not translate or identify the content, just transcribe each word phonetically."""

    with open(audio_path, 'rb') as audio_file:
        # Use prompt to force phonetic transcription instead of recognition
        transcript = client.audio.transcriptions.create(
            model="whisper-1",
            file=audio_file,
            language="en",  # Force English for romanized output
            prompt=phonetic_prompt,  # Guide to phonetic output
            response_format="verbose_json",  # Get word-level timestamps
            timestamp_granularities=["word"]
        )

    print(f"\nWhisper output: {transcript.text}")

    if hasattr(transcript, 'words'):
        print("\nWord-level timestamps:")
        for word in transcript.words:
            print(f"  [{word.start:.2f}s - {word.end:.2f}s] {word.word}")

    # Match against Surah 111
    surah = load_surah_111()
    expected_words = get_all_transliterations(surah)

    results = match_transcription(transcript.text, expected_words)

    print("\nMatching results:")
    for r in results:
        if r['status'] == 'MATCH':
            print(f"  ✅ '{r['transcribed']}' → {r['matched_arabic']} ({r['score']:.0%})")
        elif r['status'] == 'FUZZY':
            print(f"  🟡 '{r['transcribed']}' ~ {r['matched_arabic']} ({r['score']:.0%})")
        else:
            print(f"  ❌ '{r['transcribed']}' → NO MATCH")

def get_surah_romanized_prompt(surah_data):
    """Build romanized prompt from surah data."""
    words = []
    for ayah_num, ayah_data in surah_data['ayahs'].items():
        ayah_words = [w['transliteration'] for w in ayah_data['words']]
        words.extend(ayah_words)
    return ' '.join(words)


def test_gpt4o_transcribe(audio_path):
    """
    Test with gpt-4o-transcribe using romanized prompt.

    Usage:
        python romanized_transcription_poc.py path/to/audio.wav --gpt4o
    """
    try:
        from openai import OpenAI
    except ImportError:
        print("OpenAI library not installed. Run: pip install openai")
        return

    api_key = load_api_key()
    if not api_key:
        print("OPENAI_API_KEY not found")
        return

    client = OpenAI(api_key=api_key)

    # Load Surah 111 and get romanized text
    surah = load_surah_111()
    romanized_text = get_surah_romanized_prompt(surah)

    print(f"\nSurah 111 - {surah['surah_name_en']}")
    print(f"Romanized reference: {romanized_text}")
    print(f"\nTranscribing: {audio_path}")

    # Just use romanized text as prompt context
    prompt = romanized_text

    print(f"\nPrompt: {prompt[:80]}...")

    with open(audio_path, 'rb') as audio_file:
        transcript = client.audio.transcriptions.create(
            model="gpt-4o-transcribe",
            file=audio_file,
            prompt=prompt,
            language="en",  # Force English/Latin output
        )

    transcription = transcript.text
    print(f"\ngpt-4o-transcribe output: {transcription}")

    # Match against Surah 111
    expected_words = get_all_transliterations(surah)

    print(f"\nExpected words: {len(expected_words)}")
    print(f"Transcribed words: {len(transcription.strip().split())}")

    results = match_transcription(transcription, expected_words)

    print("\nMatching results:")
    matches = sum(1 for r in results if r['status'] in ['MATCH', 'FUZZY'])
    print(f"Matched: {matches}/{len(expected_words)} expected words\n")

    for r in results:
        if r['status'] == 'MATCH':
            print(f"  ✅ '{r['transcribed']}' → {r['matched_arabic']} ({r['score']:.0%})")
        elif r['status'] == 'FUZZY':
            print(f"  🟡 '{r['transcribed']}' ~ {r['matched_arabic']} ({r['score']:.0%})")
        else:
            print(f"  ❌ '{r['transcribed']}' → NO MATCH")


if __name__ == "__main__":
    if len(sys.argv) > 1:
        audio_path = sys.argv[1]

        if not os.path.exists(audio_path):
            print(f"File not found: {audio_path}")
            sys.exit(1)

        # Check for --gpt4o flag
        if "--gpt4o" in sys.argv:
            test_gpt4o_transcribe(audio_path)
        else:
            test_openai_whisper_transcription(audio_path)
    else:
        # Run simulated tests
        test_with_simulated_transcription()
        print("\n" + "=" * 60)
        print("To test with real audio:")
        print("  python romanized_transcription_poc.py your_audio.wav")
        print("  python romanized_transcription_poc.py your_audio.wav --gpt4o")
        print("=" * 60)
