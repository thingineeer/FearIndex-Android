#!/usr/bin/env bash
# Validate Google Play screenshot assets under fastlane metadata.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
META="$ROOT/fastlane/metadata/android"

failures=0

check_bucket() {
  local bucket="$1"
  local expected_count="$2"
  local expected_dimension="${3:-}"

  local count
  count=$(find "$META" -path "*/$bucket/*.png" -type f | wc -l | tr -d ' ')
  if [ "$count" != "$expected_count" ]; then
    echo "✗ $bucket count: $count (expected $expected_count)"
    failures=$((failures + 1))
  else
    echo "✓ $bucket count: $count"
  fi

  local empty_count
  empty_count=$(find "$META" -path "*/$bucket/*.png" -type f -size 0 | wc -l | tr -d ' ')
  if [ "$empty_count" != "0" ]; then
    echo "✗ $bucket empty files: $empty_count"
    find "$META" -path "*/$bucket/*.png" -type f -size 0
    failures=$((failures + 1))
  else
    echo "✓ $bucket empty files: 0"
  fi

  local dim_file
  dim_file=$(mktemp)
  while IFS= read -r file; do
    local width height
    width=$(sips -g pixelWidth "$file" 2>/dev/null | awk '/pixelWidth/ {print $2}')
    height=$(sips -g pixelHeight "$file" 2>/dev/null | awk '/pixelHeight/ {print $2}')
    if [ -z "$width" ] || [ -z "$height" ]; then
      echo "unreadable $file" >> "$dim_file"
    else
      echo "${width}x${height} $file" >> "$dim_file"
    fi
  done < <(find "$META" -path "*/$bucket/*.png" -type f | sort)

  if grep -q '^unreadable ' "$dim_file"; then
    echo "✗ $bucket unreadable PNGs:"
    grep '^unreadable ' "$dim_file"
    failures=$((failures + 1))
  fi

  if [ -n "$expected_dimension" ]; then
    local bad_count
    bad_count=$(awk -v expected="$expected_dimension" '$1 != expected { count++ } END { print count + 0 }' "$dim_file")
    if [ "$bad_count" != "0" ]; then
      echo "✗ $bucket dimensions differ from $expected_dimension:"
      awk -v expected="$expected_dimension" '$1 != expected { print }' "$dim_file" | sed -n '1,20p'
      failures=$((failures + 1))
    else
      echo "✓ $bucket dimensions: $expected_dimension"
    fi
  else
    local unique_dimensions
    unique_dimensions=$(awk '!/^unreadable / { print $1 }' "$dim_file" | sort -u | tr '\n' ' ')
    echo "✓ $bucket dimensions: $unique_dimensions"
  fi

  rm -f "$dim_file"
}

check_bucket "phoneScreenshots" 225 "1080x2400"
check_bucket "sevenInchScreenshots" 180
check_bucket "tenInchScreenshots" 180

if [ "$failures" -ne 0 ]; then
  echo "✗ screenshot validation failed: $failures issue(s)"
  exit 1
fi

echo "✓ screenshot validation passed"
