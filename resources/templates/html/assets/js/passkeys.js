//
// Passkeys
//
// This could definitely be improved - presently it just console logs out the error. It would be nice to handle appropriately and be friendly to user...
//
const { browserSupportsWebAuthn, browserSupportsWebAuthnAutofill, startRegistration, startAuthentication } = SimpleWebAuthnBrowser;

const passKeyConditionalLogin = async () => {
    if (browserSupportsWebAuthn() && browserSupportsWebAuthnAutofill()) {
        console.log("Browser supports WebAuthn and WebAuthnAutofill - w00t");
        try {
            const response = await fetch("/api/webauthn/authentication/start")
            const authenticationId = response.headers.get("x-authentication-id");
            const authenticationOptions = await response.json();
            try {
                const authenticationResponse = await startAuthentication({ optionsJSON: authenticationOptions.publicKey, useBrowserAutofill: true });
                const verificationResponse = await fetch(`/api/webauthn/authentication/verify/${authenticationId}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(authenticationResponse)
                });
                const verificationJson = await verificationResponse.json();
                if (verificationJson && verificationJson.verified) {
                    window.location = verificationJson.redirect;
                } else {
                    console.log(JSON.stringify(verificationJson));
                }
            } catch (error) {
                console.error(error);
            }
        } catch (error) {
            console.error(error);
        }
    }
};

//
// This could definitely be improved - presently it just console logs out the error. It would be nice to handle appropriately and be friendly to user...
//
const passKeyRegister = async () => {
    if (browserSupportsWebAuthn()) {
        console.log("Browser supports WebAuthn - w00t");
        try {
            const response = await fetch("/api/webauthn/registration/start");
            const registrationId = response.headers.get("x-registration-id");
            const registrationOptions = await response.json();
            try {
                const registrationResponse = await startRegistration({ optionsJSON :registrationOptions.publicKey });
                const verificationResponse = await fetch(`/api/webauthn/registration/verify/${registrationId}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(registrationResponse)
                });
                const verificationJson = await verificationResponse.json();
                if (verificationJson && verificationJson.verified) {
                    console.log("success");
                } else {
                    console.log(JSON.stringify(verificationJson));
                }
            } catch (error) {
                console.error(error);
                throw error;
            }
        } catch (error) {
            console.error(error);
        }
    }
};
