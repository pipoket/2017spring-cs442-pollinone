var pollInOneDTMF = (function() {
    var toneMap = {
        "1": [2693, 4190],
        "2": [2844, 4190],
        "3": [3105, 4190],
        "4": [2692, 4549],
        "5": [2844, 4549],
        "6": [3105, 4549],
        "7": [2692, 4925],
        "8": [2844, 4925],
        "9": [3105, 4925],
        "0": [2692, 5433],
        "P": [2844, 5433]
    };

    function playTone(startTime, toneTime, character, tone) {
        setTimeout(function() {
            console.log(character, tone);
            dialTone(tone[0], tone[1]);
        }, startTime);
        setTimeout(function() {
            stop();
        }, startTime + toneTime);
    }

    var sendText = function(text, toneTime, delayTime, preambleTime, repeat) {
        var startTime = 0;

        for(var r = 0; r < repeat; r++) {
            // Preamble
            var ptone1 = toneMap['P'][0];
            var ptone2 = toneMap['P'][1];
            setTimeout(function() {
                dialTone(ptone1, ptone2);
            }, startTime);
            setTimeout(function() {
                stop();
            }, startTime + preambleTime);
            startTime = startTime + preambleTime + delayTime;

            for(var i = 0; i < text.length; i++) {
                switch(text[i]) {
                    case "0":
                    case "1":
                    case "2":
                    case "3":
                    case "4":
                    case "5":
                    case "6":
                    case "7":
                    case "8":
                    case "9":
                        playTone(startTime, toneTime, text[i], toneMap[text[i]]);
                        startTime = startTime + toneTime + delayTime;
                        break;
                    default:
                        break;
                }
            }
        }
    };

    return {
        sendText: sendText
    };
})();


var pollInOneSTone = (function() {
    var encodeList = [
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
        "a", "b", "c", "d", "e", "f"
    ];
    var toneMap = {
        "0": 880,
        "1": 1046,
        "2": 1174,
        "3": 1318,
        "4": 1480,
        "5": 1661,
        "6": 1864,
        "7": 2093,
        "8": 2349,
        "9": 2637,
        "a": 2960,
        "b": 3322,
        "c": 3729,
        "d": 4186,
        "e": 4698,
        "f": 5274,
        "g": 5774,
    };

    var isPlaying = false;
    var timerList = [];

    function playTone(startTime, toneTime, delayTime, character) {
        var toneFreq = toneMap[character];
        if (toneFreq === undefined)
            return startTime;

        timerList.push(setTimeout(function() {
            console.log(character, toneFreq);
            toneGenerator.playTone(toneFreq);
        }, startTime));
        timerList.push(setTimeout(function() {
            toneGenerator.stop();
        }, startTime + toneTime));
        return startTime + toneTime + delayTime;
    }

    function playPreamble(startTime, preambleTime, delayTime) {
        var preambleString = "g";
        for(var i = 0; i < preambleString.length; i++)
            startTime = playTone(startTime, preambleTime, delayTime, preambleString[i]);
        return startTime;
    }

    function sendText(text, toneTime, delayTime, preambleTime, repeat) {
        var startTime = 0;

        for(var r = 0; r < repeat; r++) {
            // Preamble
            startTime = playPreamble(startTime, preambleTime, delayTime);
            // Data
            for(var i = 0; i < text.length; i++)
                startTime = playTone(startTime, toneTime, delayTime, text[i]);
        }
    };

    var sendNumber = function(strNumber, toneTime, delayTime, preambleTime, repeat) {
        if (isPlaying)
            return;

        isPlaying = true;
        var number = parseInt(strNumber);
        var hexStrNumber = "";
        while (number > 0) {
            var quo = Math.floor(number / 16);
            var rem = number % 16;
            hexStrNumber = encodeList[rem] + hexStrNumber;
            number = quo;
        }

        while (hexStrNumber.length < 8) {
            hexStrNumber = "0" + hexStrNumber;
        }
        console.log("strNumber " + strNumber + " => 0x" + hexStrNumber);
        sendText(hexStrNumber, toneTime, delayTime, preambleTime, repeat);
    }

    var stopSending = function() {
        if (isPlaying) {
            isPlaying = false;
            for(var i = 0; i < timerList.length; i++) {
                clearTimeout(timerList[i]);
            }
            toneGenerator.stop();
        }
    }

    return {
        sendNumber: sendNumber,
        stopSending: stopSending
    };
})();
