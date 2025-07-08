#!/usr/bin/env python3

import os
import argparse


def extract_text_from_folder(folder_path: str, output_file: str) -> None:
    """
    Walk through all files in the given folder (and subfolders), read their text content,
    and write each filename and its content to the output file.
    """
    with open(output_file, 'w', encoding='utf-8') as out:
        for root, dirs, files in os.walk(folder_path):
            for filename in files:
                filepath = os.path.join(root, filename)
                # Write a header for each file
                out.write(f"=== {filepath} ===\n")
                try:
                    # Attempt to read file as text
                    with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
                        content = f.read()
                except Exception as e:
                    out.write(f"[Error reading file: {e}]\n")
                else:
                    out.write(content)
                out.write("\n\n")
    print(f"Extraction complete. Output saved to: {output_file}")


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description="Extract text from all files in a folder and save to a single output file"
    )
    parser.add_argument(
        'folder',
        help='Path to the folder containing files to process'
    )
    parser.add_argument(
        '-o', '--output',
        default='extracted_text.txt',
        help='Path for the output text file (default: extracted_text.txt)'
    )
    args = parser.parse_args()
    extract_text_from_folder(args.folder, args.output)
