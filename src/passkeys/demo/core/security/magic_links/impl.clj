(ns passkeys.demo.core.security.magic-links.impl
  {:author "David Harrigan"}
  (:require
   [passkeys.demo.core.security.magic-links.repository :as repository]
   [passkeys.demo.core.utils.text :as text-utils]))

(set! *warn-on-reflection* true)

(defn get-magic-link
  [token system]
  (repository/get-magic-link token system))

(defn create-and-send-magic-link
  [account system]
  (let [token (text-utils/random-id)]
    (repository/save-magic-link token account system)
    token))
