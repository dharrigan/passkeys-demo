(ns passkeys.demo.core.security.model.principal
  {:author "David Harrigan"})

(set! *warn-on-reflection* true)

(defprotocol Principal
  (id [_])
  (first-name [_])
  (last-name [_])
  (email [_])
  (user-handle [_])
  (permissions [_]))

(defrecord Account [principal]
  Principal
  (id [_] (get principal :account/account-id))
  (first-name [_] (get principal :account/first-name))
  (last-name [_] (get principal :account/last-name))
  (email [_] (get principal :account/email))
  (user-handle [_] (get principal :account/user-handle))
  (permissions [_]
    (let [{:account/keys [is-enabled is-account-locked is-account-expired has-credentials-expired]} principal]
      {:is-enabled is-enabled :is-account-locked is-account-locked :is-account-expired is-account-expired :has-credentials-expired has-credentials-expired})))
