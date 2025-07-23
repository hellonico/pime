(ns hello3
  (:import [javax.swing JFrame JLabel Timer]
           [java.awt Font Color GraphicsEnvironment]
           [java.awt.event ActionListener]))

(defn -main []
  ;; Create window and label
  (let [frame (JFrame. "Hello LCD")
        label (doto (JLabel. "Hello!" JLabel/CENTER)
                (.setFont (Font. "SansSerif" Font/BOLD 80))
                (.setForeground Color/WHITE))
        gd (.getDefaultScreenDevice (GraphicsEnvironment/getLocalGraphicsEnvironment))]

    ;; Configure the frame
    (doto frame
      (.setUndecorated true)
      (.setBackground Color/BLACK)
      (.add label)
      (.setVisible true))

    ;; Force true fullscreen mode
    (.setFullScreenWindow gd frame)

    ;; Create blinking effect
    (let [visible? (atom true)
          timer (Timer. 500
                        (proxy [ActionListener] []
                          (actionPerformed [_]
                            (if @visible?
                              (.setText label "")
                              (.setText label "Hello!"))
                            (swap! visible? not))))]
      (.start timer))))

