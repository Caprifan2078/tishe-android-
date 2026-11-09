#!/usr/bin/env python3
"""Recover measured timings from Gradle's per-test logcat reports.

The connected Android test task may uninstall the app, invalidating adb run-as.
Logcat evidence is retained by UTP and archived by GitHub Actions.
"""
import json
import re
import sys
from pathlib import Path

def extract(results_dir: Path) -> dict:
    matches = list(results_dir.rglob('logcat-ru.tishe.app.AssessmentSaveLatencyTest-*.txt'))
    if len(matches) != 1:
        raise RuntimeError(f'Expected one QA latency logcat file, found {len(matches)}')
    raw = matches[0].read_text(encoding='utf-8', errors='replace')
    lines = re.findall(r'TISHE_QA_WRITE_LATENCY\s+(\{[^\r\n]+\})', raw)
    if len(lines) != 4:
        raise RuntimeError(f'Expected 4 instrumented measurements, found {len(lines)}')
    measurements = [json.loads(line) for line in lines]
    sizes = [item['questionCount'] for item in measurements]
    if sizes != [40, 150, 300, 500]:
        raise RuntimeError(f'Unexpected assessment sizes: {sizes}')
    for record in measurements:
        if record['samples'] != 32:
            raise RuntimeError(f'Unexpected sample count: {record}')
        if not (0 < record['minMs'] <= record['medianMs'] <= record['p95Ms'] <= record['maxMs']):
            raise RuntimeError(f'Invalid timing order: {record}')
    return {
        'format': 'tishe-assessment-main-thread-latency-v1',
        'runner': 'GitHub Actions Android emulator, API 35, Pixel 6 profile',
        'timingScope': 'AES-GCM + JSON snapshot + SharedPreferences.commit on main thread',
        'samplesPerSize': 32,
        'warmupPerSize': 4,
        'testLogSource': matches[0].name,
        'measurements': measurements,
    }

def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit('usage: extract-qa-assessment-latency.py <androidTest-results-root> <output.json>')
    result = extract(Path(sys.argv[1]))
    Path(sys.argv[2]).write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, ensure_ascii=False, indent=2))

if __name__ == '__main__':
    main()
