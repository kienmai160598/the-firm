package com.kien.movies.addons.phone

/**
 * The page the phone opens from the QR code (docs/android-tv-rebuild.md, 5b): one field for up to
 * 10 manifest URLs, one per line, and Send. It proposes the URLs, then polls the proposal until
 * the TV answers and shows the result per URL. It only adds addons; removing and ordering stay on
 * the TV.
 *
 * The token comes from the page's own address (`?t=`). Names and errors from the TV are written
 * with `textContent`, never as markup. Polling stops at the answer, so an idle session can time
 * out.
 */
object PhoneAddonPage {
    val html: String = """
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Movies: add addons</title>
<style>
  body { font-family: system-ui, sans-serif; background: #1b1d22; color: #fff; margin: 0; padding: 24px; }
  h1 { font-size: 22px; margin: 0 0 8px; }
  p { color: #b5b8be; line-height: 1.4; margin: 0 0 16px; }
  textarea { box-sizing: border-box; width: 100%; min-height: 140px; font-size: 16px; padding: 12px;
    border-radius: 12px; border: 1px solid #44484f; background: #24282e; color: #fff; }
  button { margin-top: 12px; width: 100%; font-size: 17px; font-weight: 600; padding: 14px;
    border: 0; border-radius: 12px; background: #fff; color: #000; }
  button:disabled { opacity: 0.5; }
  #status { margin-top: 20px; font-weight: 600; }
  ul { padding-left: 20px; }
  li { margin: 8px 0; word-break: break-all; }
  .failed { color: #ff8a80; }
</style>
</head>
<body>
<h1>Add addons to your TV</h1>
<p>Paste manifest URLs, one per line (at most 10). stremio:// links work too. You confirm each install on the TV.</p>
<textarea id="urls" placeholder="https://example.com/manifest.json" autocapitalize="off" autocorrect="off" spellcheck="false"></textarea>
<button id="send">Send to TV</button>
<div id="status"></div>
<ul id="results"></ul>
<script>
(function () {
  var token = new URLSearchParams(location.search).get('t') || '';
  var field = document.getElementById('urls');
  var send = document.getElementById('send');
  var status = document.getElementById('status');
  var results = document.getElementById('results');

  function say(text) { status.textContent = text; }

  function showResults(list) {
    results.textContent = '';
    list.forEach(function (result) {
      var item = document.createElement('li');
      if (result.ok) {
        item.textContent = 'Installed ' + result.name;
      } else {
        item.className = 'failed';
        item.textContent = result.url + ': ' + result.error;
      }
      results.appendChild(item);
    });
  }

  function poll(id) {
    fetch('/api/status/' + id + '?t=' + encodeURIComponent(token), { cache: 'no-store' })
      .then(function (response) { return response.json(); })
      .then(function (answer) {
        if (answer.status === 'pending') { say('Confirm on the TV.'); setTimeout(function () { poll(id); }, 1500); }
        else if (answer.status === 'installing') { say('Installing on the TV.'); setTimeout(function () { poll(id); }, 1500); }
        else if (answer.status === 'done') { say('Done.'); showResults(answer.results || []); send.disabled = false; }
        else if (answer.status === 'cancelled') { say('Cancelled on the TV. Nothing was added.'); send.disabled = false; }
        else { say('The TV no longer knows this request. Send it again.'); send.disabled = false; }
      })
      .catch(function () { say('The TV stopped answering. Open Add from phone on the TV again.'); send.disabled = false; });
  }

  send.addEventListener('click', function () {
    var urls = field.value.split('\n').map(function (line) { return line.trim(); }).filter(Boolean);
    if (urls.length === 0) { say('Paste at least one manifest URL.'); return; }
    if (urls.length > 10) { say('Send at most 10 URLs at a time.'); return; }
    send.disabled = true;
    results.textContent = '';
    say('Sending.');
    fetch('/api/addons?t=' + encodeURIComponent(token), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ urls: urls })
    })
      .then(function (response) {
        return response.json().then(function (answer) { return { ok: response.ok, answer: answer }; });
      })
      .then(function (reply) {
        if (reply.ok) { poll(reply.answer.id); }
        else { say(reply.answer.error || 'The TV refused this request.'); send.disabled = false; }
      })
      .catch(function () { say('The TV did not answer. Is the phone on the same network?'); send.disabled = false; });
  });
})();
</script>
</body>
</html>
    """.trimIndent()
}
