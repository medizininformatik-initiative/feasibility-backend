# Folder to add availabilty update files


## Example file structure for update .json files

Note that elastic search accepts this file structure of newline delimited json input even though the document as a whole is not a valid json.
The files must be terminated by a newline.

```
{"update": {"_id": "3c94e2d6-2e27-35b2-aeb7-90a476efd099"}}
{"doc": {"availability": 1000}}
{"update": {"_id": "017ec73f-65f9-3f09-99ac-d27ce04a3875"}}
{"doc": {"availability": 0}}
{"update": {"_id": "cba79ec5-7a8b-33d4-9457-9747c7b4035a"}}
{"doc": {"availability": 0}}
{"update": {"_id": "0b39df9d-91bf-35a6-bd5b-9d5e21d611d2"}}
{"doc": {"availability": 0}}
{"update": {"_id": "fad536d0-cb09-342c-af41-311822a79f0e"}}
{"doc": {"availability": 0}}
{"update": {"_id": "ad09df7a-e33d-3910-a581-7b9b365117da"}}
{"doc": {"availability": 100}}
{"update": {"_id": "8550edea-8e1a-3a3b-bfa5-f6a5ca759b39"}}
{"doc": {"availability": 0}}
{"update": {"_id": "8c9885ce-9813-3057-8281-4d0fe8f27d2a"}}
{"doc": {"availability": 0}}
{"update": {"_id": "b7d623d8-14d9-3b02-a719-0f6b81cda017"}}
{"doc": {"availability": 100}}
{"update": {"_id": "69b733e2-99eb-3a12-bcd1-8e7b16c750ae"}}
{"doc": {"availability": 10000}}

```