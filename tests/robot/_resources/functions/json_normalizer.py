def normalize_json(json_data):
    """Normalize the JSON data by:
   - Removing 'value' fields for '_type': 'DV_DATE_TIME'.
   - Ignoring 'uid' fields entirely.
   """
    if isinstance(json_data, dict):
        if json_data.get("_type") == "DV_DATE_TIME" and "value" in json_data:
            json_data["value"] = None  # Set the value to None
        if json_data.get("_type") == "DV_IDENTIFIER":
            json_data["id"] = None  # Set the value to None
        if "uid" in json_data:
            json_data["uid"] = None  # Set the uid to None or remove it for comparison
        for key, value in json_data.items():
            json_data[key] = normalize_json(value)
    elif isinstance(json_data, list):
        json_data = [normalize_json(item) for item in json_data]
    return json_data