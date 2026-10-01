// Grove passkey flows against Spring Security 7's WebAuthn endpoints.
// JSON shapes per Spring Security reference: options come back with base64url
// binary fields; register POST wraps the credential in {publicKey:{credential,label}}.
(function () {
  function csrf() {
    return document.querySelector('meta[name="csrf-token"]').content;
  }

  function b64uToBuf(s) {
    s = s.replace(/-/g, '+').replace(/_/g, '/');
    const bin = atob(s + '='.repeat((4 - (s.length % 4)) % 4));
    const buf = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i++) buf[i] = bin.charCodeAt(i);
    return buf;
  }

  function bufToB64u(b) {
    const bytes = new Uint8Array(b);
    let bin = '';
    for (let i = 0; i < bytes.length; i++) bin += String.fromCharCode(bytes[i]);
    return btoa(bin).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  }

  async function post(url, body) {
    const r = await fetch(url, {
      method: 'POST',
      headers: { 'X-CSRF-TOKEN': csrf(), 'Content-Type': 'application/json' },
      body: JSON.stringify(body ?? {})
    });
    if (!r.ok) throw new Error(url + ' responded ' + r.status);
    return r.json();
  }

  async function register(label) {
    const o = await post('/webauthn/register/options');
    const cred = await navigator.credentials.create({
      publicKey: {
        challenge: b64uToBuf(o.challenge),
        rp: o.rp,
        user: { id: b64uToBuf(o.user.id), name: o.user.name, displayName: o.user.displayName },
        pubKeyCredParams: o.pubKeyCredParams,
        timeout: o.timeout,
        excludeCredentials: (o.excludeCredentials || []).map(c => ({ id: b64uToBuf(c.id), type: c.type, transports: c.transports })),
        authenticatorSelection: o.authenticatorSelection,
        attestation: o.attestation,
        extensions: o.extensions
      }
    });
    await post('/webauthn/register', {
      publicKey: {
        label: label,
        credential: {
          id: cred.id,
          rawId: bufToB64u(cred.rawId),
          type: cred.type,
          clientExtensionResults: cred.getClientExtensionResults ? cred.getClientExtensionResults() : {},
          authenticatorAttachment: cred.authenticatorAttachment,
          response: {
            attestationObject: bufToB64u(cred.response.attestationObject),
            clientDataJSON: bufToB64u(cred.response.clientDataJSON),
            transports: cred.response.getTransports ? cred.response.getTransports() : []
          }
        }
      }
    });
  }

  async function signin() {
    const o = await post('/webauthn/authenticate/options');
    const cred = await navigator.credentials.get({
      publicKey: {
        challenge: b64uToBuf(o.challenge),
        rpId: o.rpId,
        timeout: o.timeout,
        allowCredentials: (o.allowCredentials || []).map(c => ({ id: b64uToBuf(c.id), type: c.type, transports: c.transports })),
        userVerification: o.userVerification,
        extensions: o.extensions
      }
    });
    const out = await post('/login/webauthn', {
      id: cred.id,
      rawId: bufToB64u(cred.rawId),
      type: cred.type,
      clientExtensionResults: cred.getClientExtensionResults ? cred.getClientExtensionResults() : {},
      authenticatorAttachment: cred.authenticatorAttachment,
      response: {
        authenticatorData: bufToB64u(cred.response.authenticatorData),
        clientDataJSON: bufToB64u(cred.response.clientDataJSON),
        signature: bufToB64u(cred.response.signature),
        userHandle: cred.response.userHandle ? bufToB64u(cred.response.userHandle) : null
      }
    });
    return out.redirectUrl || '/home';
  }

  async function remove(credentialId) {
    const r = await fetch('/webauthn/register/' + encodeURIComponent(credentialId), {
      method: 'DELETE',
      headers: { 'X-CSRF-TOKEN': csrf() }
    });
    if (!r.ok) throw new Error('delete responded ' + r.status);
  }

  window.groveWebAuthn = { register, signin, remove };
})();
