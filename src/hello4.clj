(ns hello4
  (:import [javax.swing JFrame JLabel Timer]
           [java.awt Font Color GraphicsEnvironment]
           [java.awt.event ActionListener])
  (:require [clojure.string :as str]))

(defn format-time [seconds]
  (let [m (quot seconds 60)
        s (mod seconds 60)]
    (format "%02d:%02d" m s)))

(defn -main []
  (let [frame (JFrame. "Timer")
        label (doto (JLabel. "00:00" JLabel/CENTER)
                (.setFont (Font. "SansSerif" Font/BOLD 100))
                (.setForeground Color/WHITE))
        gd (.getDefaultScreenDevice (GraphicsEnvironment/getLocalGraphicsEnvironment))
        counter (atom 0)]  ;; timer starts at 0 seconds

    ;; Configure the frame
    (doto frame
      (.setUndecorated true)
      (.setBackground Color/BLACK)
      (.add label)
      (.setVisible true))

    ;; Fullscreen mode
    (.setFullScreenWindow gd frame)

    ;; Timer to update every second
    (let [timer (Timer. 1000
                        (proxy [ActionListener] []
                          (actionPerformed [_]
                            (swap! counter inc)
                            (.setText label (format-time @counter)))))]
      (.start timer))))

