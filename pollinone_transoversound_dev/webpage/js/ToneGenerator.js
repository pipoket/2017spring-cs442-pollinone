var toneGenerator = (function() {
      var contextClass = (window.AudioContext ||
        window.webkitAudioContext ||
        window.mozAudioContext ||
        window.oAudioContext ||
        window.msAudioContext);

      if (contextClass) {
        // Web Audio API is available.
        var context = new contextClass();
      }

      var oscillator1;

      var playTone = function(freq1, freq2){
        oscillator1 = context.createOscillator();
        oscillator1.type = 'triangle';
        oscillator1.frequency.value = freq1;
        gainNode = context.createGain ? context.createGain() : context.createGainNode();
        oscillator1.connect(gainNode,0,0);
        gainNode.connect(context.destination);
        gainNode.gain.value = .5;
        oscillator1.start ? oscillator1.start(0) : oscillator1.noteOn(0)
      };

      function stop() {
        oscillator1.disconnect();
      }

      return {
          playTone: playTone,
          stop: stop
      };
})();
