"""Clicks the elements the given selectors name, one after the other, in the page Janus shows, through the
debugging connection of its browser. Each is waited for, since the page swaps its content in after a click.
Exits with 1 when an element never shows up."""
import json
import sys
import time
import urllib.request

import websocket

ADDRESS = "http://127.0.0.1:9222"
WAIT_SECONDS = 60
CLICK = "(() => { const e = document.querySelector(%s); if (!e) return false; e.click(); return true; })()"


def page():
    deadline = time.time() + WAIT_SECONDS

    while time.time() < deadline:
        try:
            with urllib.request.urlopen(ADDRESS + "/json", timeout=5) as response:
                for target in json.load(response):
                    if target["type"] == "page" and target["url"].startswith("http://127.0.0.1"):
                        return target
        except OSError:
            pass

        time.sleep(1)

    sys.exit(1)


# Chromium turns away a debugging connection that names an origin it was not told to trust.
connection = websocket.create_connection(page()["webSocketDebuggerUrl"], timeout=10, suppress_origin=True)


def evaluate(expression):
    connection.send(json.dumps({"id": 1, "method": "Runtime.evaluate",
                                "params": {"expression": expression, "returnByValue": True}}))

    while True:
        message = json.loads(connection.recv())

        if message.get("id") == 1:
            return message["result"]["result"].get("value")


for selector in sys.argv[1:]:
    deadline = time.time() + WAIT_SECONDS

    while not evaluate(CLICK % json.dumps(selector)):
        if time.time() > deadline:
            sys.exit(1)

        time.sleep(1)
