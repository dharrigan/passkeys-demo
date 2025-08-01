(ns passkeys.demo.ui.login.spec
  {:author "David Harrigan"})

(set! *warn-on-reflection* true)

(def ^:private LoginRequestWithReason
  [:map
   [:reason {:optional true} [:enum "401"]]])

(def ^:private BasicLoginRequest
  [:map
   [:username :string]])

(def MagicLinkLoginRequest
  [:map
   [:token {:optional true} :string]])

(def login-request-with-reason LoginRequestWithReason)
(def basic-login-request BasicLoginRequest)
(def magic-link-login-request MagicLinkLoginRequest)
