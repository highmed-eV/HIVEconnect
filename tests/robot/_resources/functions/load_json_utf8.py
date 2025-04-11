import json

def load_json_utf8(file_path,patientid):
    """Loads JSON data from a file with UTF-8 encoding"""
    with open(file_path, 'r', encoding='utf-8') as file:
        content = json.load(file)
        # Convert the entire content to a string, replace, and load back to dict
        content_str = json.dumps(content)
        updated_str = content_str.replace('{{patientid}}', patientid)
        updated_content = json.loads(updated_str)
        
        return updated_content
    
def update_input_json(inputfile,patientid):
    """Loads JSON data from a file with UTF-8 encoding"""
    # Convert the entire content to a string, replace, and load back to dict
    content_str = json.dumps(inputfile)
    updated_str = content_str.replace('{{patientId}}', patientid)
    updated_content = json.loads(updated_str)
    
    return updated_content