(ns passkeys.demo.system.config.impl
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.system.config.schema :as schema]))

(set! *warn-on-reflection* true)

(defn apply-defaults
  [config]
  (schema/apply-defaults config))

(defn validate
  [config]
  (schema/validate config))

(defn ^:private security-config
  [system]
  (get-in system [:runtime-config :security]))

(defn cookies
  [system]
  (:cookies (security-config system)))

(defn cors
  [system]
  (:cors (security-config system)))

(defn ^:private authentication-config
  [system]
  (get-in system [:runtime-config :security :authentication]))

(defn allowed-origins
  [system]
  (:allowed-origins (authentication-config system)))

(defn ^:private webauthn-config
  [system]
  (get-in system [:runtime-config :security :webauthn]))

(defn passkeys
  [system]
  (:passkeys (webauthn-config system)))
