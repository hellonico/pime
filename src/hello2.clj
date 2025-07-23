(ns hello2
  (:import [javax.swing JFrame JLabel]
           [java.awt Font Color]
           [javax.swing Timer]))

(defn -main []
  ;; Create a fullscreen window
  (let [frame (JFrame. "Hello LCD")
        label (doto (JLabel. "Hello!" JLabel/CENTER)
                (.setFont (Font. "SansSerif" Font/BOLD 80))
                (.setForeground Color/WHITE))
        screen-size (.getScreenSize (java.awt.Toolkit/getDefaultToolkit))]

    ;; Set up the frame
    (doto frame
      (.setSize (.width screen-size) (.height screen-size))
      (.setUndecorated true)
      (.setBackground Color/BLACK)
      (.setExtendedState JFrame/MAXIMIZED_BOTH)
      (.add label)
      (.setVisible true))

    ;; Timer to blink the text
    (let [visible? (atom true)]
      (Timer. 500 ;; Blink every 500ms
              (proxy [java.awt.event.ActionListener] []
                (actionPerformed [_]
                  (if @visible?
                    (.setText label "")
                    (.setText label "Hello!"))
                  (swap! visible? not))))
      (.start (Timer. 500
                      (proxy [java.awt.event.ActionListener] []
                        (actionPerformed [_]
                          (if @visible?
                            (.setText label "")
                            (.setText label "Hello!"))
                          (swap! visible? not))))))))
(-main)

