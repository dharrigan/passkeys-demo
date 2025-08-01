(ns passkeys.demo.api.routes
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.api.security.webauthn.routes :as webauthn-api]))

(set! *warn-on-reflection* true)

(defn ^:private public-routes
  []
  [(webauthn-api/routes)])

(defn routes
  []
  ["/api" (public-routes)])
