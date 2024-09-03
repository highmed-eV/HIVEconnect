def normalize_json(json_data):
    """Normalize the JSON data by removing 'value' fields for DV_DATE_TIME types."""
    if isinstance(json_data, dict):
        if json_data.get("_type") == "DV_DATE_TIME":
            if "value" in json_data:
                json_data["value"] = None  # Set the value to None (or you can remove it)
        for key, value in json_data.items():
            json_data[key] = normalize_json(value)
    elif isinstance(json_data, list):
        json_data = [normalize_json(item) for item in json_data]
    return json_data