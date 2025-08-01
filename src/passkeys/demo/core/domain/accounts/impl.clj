(ns passkeys.demo.core.domain.accounts.impl
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.core.domain.accounts.repository :as repository]))

(set! *warn-on-reflection* true)

(defn get-account-by-email
  [email system]
  (repository/get-account-by-email email system))

(defn get-account-by-user-handle
  [user-handle system]
  (repository/get-account-by-user-handle user-handle system))

(defn get-credential-repository
  [system]
  (repository/get-credential-repository system))

(defn save-passkey-registration-request-options
  [registration-id registration-request-options system]
  (repository/save-passkey-registration-request-options registration-id registration-request-options system))

(defn get-passkey-registration-request-options
  [registration-id system]
  (repository/get-passkey-registration-request-options registration-id system))

(defn save-passkey-authentication-request-options
  [authentication-id authentication-request-options system]
  (repository/save-passkey-authentication-request-options authentication-id authentication-request-options system))

(defn get-passkey-authentication-request-options
  [authentication-id system]
  (repository/get-passkey-authentication-request-options authentication-id system))

(defn save-passkey-credential
  [registration-id account registration-request-options registration-result system]
  (repository/save-passkey-credential registration-id account registration-request-options registration-result system))

(defn update-passkey-credential
  [authentication-id account assertion-result system]
  (repository/update-passkey-credential authentication-id account assertion-result system))
