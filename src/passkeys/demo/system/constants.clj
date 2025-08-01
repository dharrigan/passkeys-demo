(ns passkeys.demo.system.constants
  {:author "David Harrigan"})

(set! *warn-on-reflection* true)

(def application-json "application/json")
(def bad-request 400)
(def content-type "Content-Type")
(def html "text/html")
(def internal-server-error 500)
(def passkeys-demo-magic-link "passkeys-demo:magic-link:")
(def passkeys-demo-passkey-authentication-request "passkeys-demo:passkey-authentication-request:")
(def passkeys-demo-passkey-registration-request "passkeys-demo:passkey-registration-request:")
(def passkeys-demo-session "passkeys-demo:session:")
(def unauthorized 401)
(def x-authentication-id "x-authentication-id")
(def x-registration-id "x-registration-id")
