#!/usr/bin/env python3
"""Upload a Nibras Nova AAB (and optional R8 mapping) to a Google Play track.

Requires: pip install google-api-python-client google-auth
Env:  PLAY_SERVICE_ACCOUNT_JSON  path to the service-account JSON file, or the JSON text itself.
Usage: scripts/play_upload.py app-release.aab [--mapping mapping.txt] [--track internal] [--package com.nibras.nova]
The service account needs "Release to testing tracks" permission in Play Console.
"""
import argparse, json, os, sys
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]

def credentials():
    raw = os.environ.get("PLAY_SERVICE_ACCOUNT_JSON")
    if not raw:
        sys.exit("PLAY_SERVICE_ACCOUNT_JSON is not set")
    info = json.load(open(raw)) if os.path.isfile(raw) else json.loads(raw)
    return service_account.Credentials.from_service_account_info(info, scopes=SCOPES)

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("aab")
    ap.add_argument("--mapping")
    ap.add_argument("--track", default="internal")
    ap.add_argument("--package", default="com.nibras.nova")
    ap.add_argument("--status", default="completed", help="completed | draft (draft needed while the app is unpublished)")
    ap.add_argument("--notes", default="")
    a = ap.parse_args()

    svc = build("androidpublisher", "v3", credentials=credentials(), cache_discovery=False)
    edits = svc.edits()
    edit_id = edits.insert(packageName=a.package, body={}).execute()["id"]
    bundle = edits.bundles().upload(packageName=a.package, editId=edit_id,
        media_body=MediaFileUpload(a.aab, mimetype="application/octet-stream", resumable=True)).execute()
    vc = bundle["versionCode"]
    print("Uploaded bundle versionCode", vc)
    if a.mapping:
        edits.deobfuscationfiles().upload(packageName=a.package, editId=edit_id, apkVersionCode=vc,
            deobfuscationFileType="proguard",
            media_body=MediaFileUpload(a.mapping, mimetype="application/octet-stream", resumable=True)).execute()
        print("Uploaded mapping.txt")
    release = {"versionCodes": [str(vc)], "status": a.status}
    if a.notes:
        release["releaseNotes"] = [{"language": "en-US", "text": a.notes}]
    edits.tracks().update(packageName=a.package, editId=edit_id, track=a.track,
        body={"track": a.track, "releases": [release]}).execute()
    edits.commit(packageName=a.package, editId=edit_id).execute()
    print(f"Committed to track '{a.track}' ({a.status})")

if __name__ == "__main__":
    main()
