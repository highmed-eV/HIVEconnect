import json

def load_json_utf8(file_path):
    """Loads JSON data from a file with UTF-8 encoding"""
    with open(file_path, 'r', encoding='utf-8') as file:
        return json.load(file)