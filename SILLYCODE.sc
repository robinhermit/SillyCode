~execute.(); // this line has to be executed after the rest

( // FIRST EXECUTE THIS BLOCK (wait until server is booted)
~aa = {

	// automate this!!!!
	var python_path = "/Library/Frameworks/Python.framework/Versions/3.11/bin/python3.11";

	// path to current folder
	~path = thisProcess.nowExecutingPath.dirname;

	~get_sylabs = {arg language, input;
		format(
			// check python path! otherwise unixCmdGetStdOut won't work
			python_path++" -c \"import pyphen; out = pyphen.Pyphen(left=1, right=1, lang='%').inserted('%'); print(out)\"",
			language, input
		).unixCmdGetStdOut.split($-);
	};

	~get_more = {|language, input|
		format(
			python_path++" -c \"import pyphen; dic= pyphen.Pyphen(lang='%'); out = dic.iterate('%'); print(tuple(out))\"",
			language, input
		).unixCmdGetStdOut.split($-)
	};

	~get_position = {|language, input|
		format(
			// check python path! otherwise unixCmdGetStdOut won't work
			python_path++" -c \"import pyphen; dic= pyphen.Pyphen(lang='%'); out = dic.positions('%'); print(out)\"",
			language, input
		).unixCmdGetStdOut.split($-)
	};


	"Parsing functions: ready.".postln;

	// test string in en
	~string = "Invisible Margaret with Wednesday vibe";
	~language= "en";

	~mode=0;

	// mode engine (letter-centric, syllable-centric)
	~mode_parser = {arg selector, current;
		var selector_value,
		current_value,
		actual_switch;

		selector_value = selector.value;
		current_value = current.value;
		actual_switch = switch(selector_value)
		{0} // letter-centric
		{
			// for each letter in the syllable
			~letters_in_syllable.do{ // put in a duration array
				arg j;
				~syllable_durations = ~syllable_durations.insert(
					j, (1/(~letters_in_syllable*~coeff_lett_multiplier)).round(~roundVal) // < < < < < < FIX THIS
				);
			};

			// get durations for durations array
			if(	// if it's the last syllable, sample it for duration
				~syllable_durations.size === ~letters_in_syllable,
				// transfer it
				{~word_durations = ~word_durations.add(~syllable_durations*~tempo_factor);
					// check for sample/syllable associations
					// prep buffer array
					// look for syllables in "unique" to find idx
					if( // if syllable is new..
						~saved_pairs.keys.asArray
						.includesEqual(~currentWord_syllables.at(current_value).asString)
						.not,
						{//.. save a new association in cached_pairs dict
							// sample picked random from the assignable set
							~bingo_sample_num = ~assignable_samples.asArray.flatten[~sounds.size.rand];
							// create current syllable association
							~saved_pairs = ~saved_pairs.put(
								~currentWord_syllables.at(current_value).asString,
								~bingo_sample_num);
							// finally, remove the random picked sample number from the set of assignable samples;
							~assignable_samples.remove(~bingo_sample_num);
							"nuova sillaba aggiunta".postln;
							// operations on buffers array
							~syl_index = ~saved_pairs.at(~currentWord_syllables.at(current_value).asString);
							~moltiplica_indice_copie = Array.fill(
								~letters_in_syllable, {~syl_index}
							);
							~indici_parola = ~indici_parola.insert(current_value, ~moltiplica_indice_copie);
						},
						{ // if not new syllable, parse its value
							if(
								~saved_pairs.keys.asArray
								.includesEqual(~currentWord_syllables.at(current_value).asString),
								{
									// stessa roba diocan?
									~syl_index = ~saved_pairs.at(~currentWord_syllables.at(current_value).asString);
									~moltiplica_indice_copie = Array.fill(
										~letters_in_syllable, {~syl_index});
									~indici_parola = ~indici_parola.insert(current_value, ~moltiplica_indice_copie);
								},
								{"uopssss....".postln}
			)})});
		}

		{1} // syllable-centric
		{
			// only 1 duration value per syllable
			~syllable_durations = ~syllable_durations.add(~letters_in_syllable*0.125);
			~word_durations = ~word_durations.add(~syllable_durations*~tempo_factor);
			//////////////////////////////////////////////////////////////////////

			if( // new syllable..
				~saved_pairs.keys.asArray
				.includesEqual(~currentWord_syllables.at(current_value).asString)
				.not,
				{//.. save association in cached_pairs
					// sample picked random from the assignable set
					~bingo_sample_num = ~assignable_samples.asArray.flatten[~sounds.size.rand];
					~saved_pairs = ~saved_pairs.put(
						~currentWord_syllables.at(current_value).asString,
						~bingo_sample_num);
					// finally, remove the random picked sample number from the set of assignable samples;
					~assignable_samples.remove(~bingo_sample_num);
					"nuova sillaba aggiunta".postln;

					~syl_index = ~saved_pairs.at(~currentWord_syllables.at(current_value).asString);
					~moltiplica_indice_copie = Array.fill(
						~letters_in_syllable, {~syl_index}
					);
					~indici_parola = ~indici_parola.insert(current_value, ~moltiplica_indice_copie);
				},

				{ // not new syllable
					if(
						~saved_pairs.keys.asArray
						.includesEqual(~currentWord_syllables.at(current_value).asString),
						{
							// stessa roba diocan?
							~syl_index = ~saved_pairs.at(~currentWord_syllables.at(current_value).asString);
							~moltiplica_indice_copie = Array.fill(
								~letters_in_syllable, {~syl_index});
							~indici_parola = ~indici_parola.insert(current_value, ~moltiplica_indice_copie);
						},
						{"uopssss....".postln}
		)})}
	};

	"SillyCode - a live-coding environment for text sonification".postln;

	Server.killAll;
	SerialPort.closeAll;

};

//////////////////////

~bb = {

	// test function

	x = ~get_sylabs.(~language, ~string).do(_.postln);
	if(
		x.isNil.not, {
			" >>> first-choice function setup!! <<<".postln;
			x.postln;
	});


	y = ~get_more.(~language, ~string).do(_.postln);
	if( y.isNil.not
		&& y.isArray
		&& y.size > 0,
	{
			" >>> alternative function setup!! <<<".postln;
			// splitta in get_more indexes (split positions)
  	});

	z = ~get_position.(~language, ~string).do(_.postln);
	if( z.isNil.not
		&& z.isArray
		&& z.size > 0,
	{
			" >>> positions setup!!! <<<".postln;
			// splitta in get_more indexes (split positions)
		z.postln;
	});
};

~cc = {// server operations
	//Server.default.options.device = "BlackHole 2ch";
	//Server.default.options.device = "Scarlett 18i20 USB";
	Server.default.options.inDevice = "Built-in Input";
	Server.default.options.outDevice = "Built-in Output";
	Server.default.options.sampleRate = 44100;
	s.options.numOutputBusChannels = 4; // 4 out hardware chs (use ~outs to set actual routing)
	s.options.numBuffers = 20000; // then reboot to save the changee
	s.options.memSize = 32768;

	s.reboot;

	s.waitForBoot{

		var sounds, folder;

		~fft_frameSize = 512; // fft analysis frame size
		~fft_hopSize = 0.25; // hop size

		Buffer.freeAll;

		~sounds = Array.new;

		folder = PathName.new(thisProcess.nowExecutingPath.dirname+/+"Samples");
		folder.entries.scramble.do({
			arg path;
			var soundfile, framerate, hopsize;

			soundfile = SoundFile.new(path.fullPath); // current soundfile to calculate framesize duration
			framerate = ~fft_frameSize;
			hopsize = ~fft_hopSize;

			~sounds = ~sounds.add(Buffer.readChannel(s, path.fullPath, channels: [0]));
		});

		// allocate buffer for live granulation
		~grain_storage = Buffer.alloc(s, s.sampleRate*3);
		~grain_storage.zero;

		// patterns out busses
		~bus0 = Bus.audio(s,1);
		~bus1 = Bus.audio(s,1);
		~bus2 = Bus.audio(s,1);
		~bus3 = Bus.audio(s,1);
		~bus4 = Bus.audio(s,1);
		~bus5 = Bus.audio(s,1);
		~bus6 = Bus.audio(s,1);
		~bus7 = Bus.audio(s,1);
		~bus8 = Bus.audio(s,1);
		~bus9 = Bus.audio(s,1);

		// other busses
		~master_bus = Bus.audio(s,4);
		~fs_buf = Bus.audio(s,4);
		~grain_buf = Bus.audio(s,4);
		~rev_bus = Bus.audio(s,4);

		// groups used in granulation
		~group1 = Group.new; // group for loops 0,1,2
		~group2 = Group.new(~group1, \addAfter); // group for loops 3,4,5
		~group3 = Group.new(~group2, \addAfter); // group for loops 6,7,8,9

		SynthDef.new(\playbuf_test_stereo, { // versione classica
			arg amp = 1, out = 0, outFreeze=0, buf,
			rate = 1, t_trig = 1, start = 0,
			atk = 0.01, rel = 0.5, da = 2,
			hicut=17000, lowcut=30,
			dryamt = 1, revamt = 0.5,
			revout = 3, pointer=0,
			framesize = ~fft_frameSize;

			var sig, env, isRev, startOpts, endPoint, frames,
			attack, release, bufdur, bufscale,
			local_buf, chain;

			bufdur = BufDur.kr(buf);
			bufscale = BufRateScale.kr(buf);
			attack = atk.clip(0.005, bufdur-(bufdur/10));
			release = rel.clip(0.1, 5);

			sig = PlayBuf.ar(1, buf, bufscale * rate, t_trig,
				start,
				loop: 0,
				doneAction: da) /*!2*/ ;

			//PV stuff
			//local_buf = LocalBuf.new(framesize); // local to store fft
			//chain = PV_BufRd(local_buf, buf, pointer);

			env = EnvGen.ar(Env.asr(attack, 1, release, -6), t_trig, doneAction: da);
			sig = sig * env;
			sig = sig * amp; // fader amp
			// outputs
			Out.ar(out, sig);
			//Out.ar(outFreeze, IFFT(chain, 1).dup);
		}).add;

		SynthDef.new(\channelBus, { // ten instances, one for each pattern
			// basically a channel strip with a bunch of insert fxs and aux send chs
			arg in=0, lowcut=20, hicut=16000,
			dryBus=0, revBus=0, crushBus=0, fsBus=0, grainBus=0, // output busses
			dryAmt=1, revAmt=0, crushAmt=0, fsAmt=0, grainAmt=0, // updated according to matrix
			sp1_amt=0.5, sp2_amt=0.5, // amps per speaker updated in task below
			sp3_amt=0, sp4_amt=0; // depends on ~outs and Ll Rr columns on matrix
			var input, sig, dist, mix, ampmap,
			main_sig, rear_sig;
			input = In.ar(in,2); // dry from looper

			// add distorsion (insert)
			sig = Normalizer.ar(input, 0.7, 0.01);
			dist = InsideOut.ar(input);
			//ampmap = Amplitude.ar(dist, 0.01, 0.1);
			// sum
			sig = ((sig*(1-crushAmt)) + (dist*crushAmt));

			sig = Compander.ar(sig, sig, thresh:0.75, slopeBelow:1.3, slopeAbove:0.4, clampTime:0.13, relaxTime: 0.2);

			// filter eq
			sig = HPF.ar(sig, lowcut.lag(0.1)); // add lowcut filter
			sig = LPF.ar(sig, hicut);// add hicut filter
		//sig = Limiter.ar(sig, 0.65, 0.05);

			sig = sig * 0.5;

			// duplicate the stereo signals, 2 pairs!
			main_sig = [
				sig[0]*sp1_amt,
				sig[1]*sp2_amt,
				sig[0]*sp3_amt,
				sig[1]*sp4_amt,
			];
			sig = main_sig;

			Out.ar(dryBus, sig*dryAmt);
			Out.ar(fsBus, sig*fsAmt);
			Out.ar(revBus, sig*revAmt);
			Out.ar(grainBus, sig*grainAmt);
		}).add;

		SynthDef.new(\reverb, {
			arg in=0, out=0, rear=1,
			roomSize=80,
			revTime=5,
			damping=0.3,
			inputbtw=0.2,
			drylevel=(-60),
			earlylevel=(-12),
			taillevel=(-21),
			amp = 0.2, hpfreq = 200,
			sp1_amt=0.5, sp2_amt=0.5,
			sp3_amt=0, sp4_amt=0;
			var sig, main_sig;
			sig = In.ar(in,4);
			sig = GVerb.ar(sig,
				roomsize: roomSize,
				revtime: revTime,
				damping: damping,
				drylevel: drylevel,
				earlyreflevel: earlylevel,
				taillevel: taillevel
			) *amp;
			sig = HPF.ar(sig, hpfreq);
			sig = Limiter.ar(sig, 0.6, 0.05);

			main_sig = [
				sig[0]*sp1_amt,
				sig[1]*sp2_amt,
				sig[2]*sp3_amt,
				sig[3]*sp4_amt,
			];

			sig = main_sig;

			Out.ar(out, sig);
			//Out.ar(rear, sig);
		}).add;

		SynthDef.new(\freqshift, {
			arg in=0, out=0, fshift=0.45,
			sp1_amt=0.5, sp2_amt=0.5,
			sp3_amt=0, sp4_amt=0;

			var sig, main_sig;
			sig = In.ar(in,4);
			sig = FreqShift.ar(sig, SinOsc.kr(fshift), 0);

			main_sig = [
				sig[0]*sp1_amt,
				sig[1]*sp2_amt,
				sig[2]*sp3_amt,
				sig[3]*sp4_amt,
			];
			sig = main_sig;

			Out.ar(out, sig);
		}).add;

		SynthDef.new(\tgrains_live, {
			arg in=0, out=0, gate=1, buf=0,
			atk=0.05, sus=1, rel=2, delay=1,
			rate=1, ratescale=1, amp=1, // only pitch down (0.0-1)
			sp1_amt=0.5, sp2_amt=0.5,
			sp3_amt=0, sp4_amt=0,
			duration=0.05;

			var input, pointer, sig, main_sig, env, pos;
			input = In.ar(in, 4);

			pointer = Phasor.ar(0,1,0,BufFrames.kr(buf));

			BufWr.ar(input, buf, pointer);

			pos = ((pointer/SampleRate.ir) - 0.25);
			pos = pos - (0,0.25..1);
			pos = pos + LFNoise1.kr(100!4).bipolar(0.25);

			sig = TGrains.ar(
				numChannels: 4,
				trigger: Dust.ar(40),
				bufnum: buf,
				rate: rate*ratescale,
				centerPos: pos,
				dur: duration,
				pan: 0,
				amp: 1,
				interp: 4
			);

			env = EnvGen.kr(Env.asr(atk, sus, rel), gate, doneAction:2);
			sig = sig * env * amp;

			main_sig = [
				sig[0]*sp1_amt,
				sig[1]*sp2_amt,
				sig[2]*sp3_amt,
				sig[3]*sp4_amt,
			];

			sig = main_sig;

			Out.ar(out, sig);
		}).add;

		SynthDef.new(\master, {
			arg in = 0, main_out = 0;
			var sig, main_sig;
			sig = In.ar(in, 4);
			Out.ar(main_out, sig);
		}).add;
	};

	"server booted!".postln;
};


////////////////////////////////////////////////////////////////////////////////////////////////////////
// G U I   F U N C T I O N S //////////////////////////////////////
/////////////////////////////////////////////////7

~execute = { // maian function executed manually after all this has been compiled

	var scale, trig_threshold, // GUI and matrix properties
	range_min, range_max,
	columns, rows, // user specified
	sound_folder_path, // user specified
	views, views_full, // handle GUI layouts & views
	play_slice, slice_threshold, slice_metric, // inside twoCorpus instrument, but also called at colorCheck rate
	middleRow, threeQuarters; // matrix locations

	scale = 60; // resize matrix window
	trig_threshold = 0.1; // thresh detection on matrix (0-1)
	range_min = 150; // min sensor value mapped
	range_max = 400; // max sensor value mapped

	columns = 15; // number of columns (to digital pins)
	rows = 14; // number of rows (to analog pins)

	// initialise tasks and serial port
	Tdef(\readSerial).stop;
	Tdef(\colorControl).stop;
	Tdef(\colorCheck).stop;
	SerialPort.closeAll;

	// open audio channels
	// (ordered as they would in a real audio chain.. tweak around)
	~freqshift_ch = Synth.new(\freqshift, [\in, ~fs_bus, \out, ~master_bus], s, \addToTail);
	~rev_ch = Synth.new(\reverb, [\in, ~rev_bus, \out, ~master_bus], s, \addToTail);
	~grain_ch = Synth(\tgrains_live, [\buf, ~grain_storage, \in, ~grain_bus, \out, ~master_bus, \gate, 1], s, \addToTail);
	~master_ch = Synth.new(\master, [\in, ~master_bus, \main_out, 0], s, \addToTail);


	// open looping channels and direct to busses
	// amt to each bus wil be calculated by a dedicated process later on
	// this way the effects are updated in real time,
	// while patterns are updated each time ENTER is hit
	~ch0 = Synth.new(\channelBus, [
		\in, ~bus0, \dryBus, ~master_bus, \revBus, ~rev_bus, \fsBus, ~fs_bus, \grainBus, ~grain_bus,], ~group1);
	~ch1 = Synth.new(\channelBus, [
		\in, ~bus1, \dryBus, ~master_bus, \revBus, ~rev_bus, \fsBus, ~fs_bus, \grainBus, ~grain_bus,], ~group1);
	~ch2 = Synth.new(\channelBus, [
		\in, ~bus2, \dryBus, ~master_bus, \revBus, ~rev_bus, \fsBus, ~fs_bus, \grainBus, ~grain_bus,], ~group1);
	~ch3 = Synth.new(\channelBus, [
		\in, ~bus3, \dryBus, ~master_bus, \revBus, ~rev_bus, \fsBus, ~fs_bus, \grainBus, ~grain_bus,], ~group2);
	~ch4 = Synth.new(\channelBus, [
		\in, ~bus4, \dryBus, ~master_bus, \revBus, ~rev_bus, \fsBus, ~fs_bus, \grainBus, ~grain_bus,], ~group2);
	~ch5 = Synth.new(\channelBus, [
		\in, ~bus5, \dryBus, ~master_bus, \revBus, ~rev_bus, \fsBus, ~fs_bus, \grainBus, ~grain_bus,], ~group2);
	~ch6 = Synth.new(\channelBus, [
		\in, ~bus6, \dryBus, ~master_bus, \revBus, ~rev_bus, \fsBus, ~fs_bus, \grainBus, ~grain_bus,], ~group3);
	~ch7 = Synth.new(\channelBus, [
		\in, ~bus7, \dryBus, ~master_bus, \revBus, ~rev_bus, \fsBus, ~fs_bus, \grainBus, ~grain_bus,], ~group3);
	~ch8 = Synth.new(\channelBus, [
		\in, ~bus8, \dryBus, ~master_bus, \revBus, ~rev_bus, \fsBus, ~fs_bus, \grainBus, ~grain_bus,], ~group3);
	~ch9 = Synth.new(\channelBus, [
		\in, ~bus9, \dryBus, ~master_bus, \revBus, ~rev_bus, \fsBus, ~fs_bus, \grainBus, ~grain_bus,], ~group3);

	// resetta parametri temporali, crea tempoclock
	~tempo = 120; ~bpm=120;
	~measure = 4; ~division=4;
	~stretch = (60/~tempo)*~measure;
	~rate = 1;

	// resetta numero speakers (4 outs are always open, but usually not used unless
	// the side quest @outs=4/
	~numSpeakers = 0; ~outs = 2;

	~clock = TempoClock.new(~tempo/60);
	~numeroCorrente=0;

	Pdef(\playbuf_1, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).play(~clock, quant:~stretch);
	Pdef(\playbuf_2, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).play(~clock, quant:~stretch);
	Pdef(\playbuf_3, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).play(~clock, quant:~stretch);
	Pdef(\playbuf_4, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).play(~clock, quant:~stretch);
	Pdef(\playbuf_5, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).play(~clock, quant:~stretch);
	Pdef(\playbuf_6, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).play(~clock, quant:~stretch);
	Pdef(\playbuf_7, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).play(~clock, quant:~stretch);
	Pdef(\playbuf_8, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).play(~clock, quant:~stretch);
	Pdef(\playbuf_9, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).play(~clock, quant:~stretch);
	Pdef(\playbuf_0, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).play(~clock, quant:~stretch);
	Pdef(\playbuf_1).fadeTime = 1;
	Pdef(\playbuf_2).fadeTime = 1;
	Pdef(\playbuf_3).fadeTime = 1;
	Pdef(\playbuf_4).fadeTime = 1;
	Pdef(\playbuf_5).fadeTime = 1;
	Pdef(\playbuf_6).fadeTime = 1;
	Pdef(\playbuf_7).fadeTime = 1;
	Pdef(\playbuf_8).fadeTime = 1;
	Pdef(\playbuf_9).fadeTime = 1;
	Pdef(\playbuf_0).fadeTime = 1;

	// resetta parametri Side Quest
	~tempo_factor = 1; ~tFact=1;
	~modeSelector = 0; ~mode=0;
	~attack = 0.01; ~atk=0.01;
	~release = 0.5; ~rel=0.5;
	~rate = 1;

	Pdef(\playbuf_1, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).stop;
	Pdef(\playbuf_2, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).stop;
	Pdef(\playbuf_3, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).stop;
	Pdef(\playbuf_4, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).stop;
	Pdef(\playbuf_5, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).stop;
	Pdef(\playbuf_6, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).stop;
	Pdef(\playbuf_7, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).stop;
	Pdef(\playbuf_8, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).stop;
	Pdef(\playbuf_9, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).stop;
	Pdef(\playbuf_0, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).stop;

	Window.closeAll;

	w = Window.new("SillyCode-Matrix", Rect(0,0,
		width: (Window.screenBounds.width/1.5)+20,
		height: Window.screenBounds.height
	)).front; // main window
	w.background_(Color.gray(0));

	w.onClose_({
		if( // SALVATAGGIO AUTOMATICO DEL TESTO SE C'è QUALCOSA DI SCRITTO
			t.string.asString.notNil,
			{
				var file,date;
				date = Date.getDate.format("%Y%m%d_%H%M");
				//~path = thisProcess.nowExecutingPath;
				file = File.new(~path+/+"Saved"+/+"SillyCode_"++date++".txt", "w");
				file.write(t.string.asString);
				file.close;
				"testo salvato".postln;
			},
			{"testo vuoto".postln}
		);
	});

	z = PdefAllGui(11);
	s.meter;



	~coeff_lett_multiplier = 2; // num.letters multiplier
	~roundVal = 0.025; // duration approximation

	~language = "en"; // default language English
	~lang = ~language;

	~word_durations = []; // empty array of word durations
	~indici_parola = []; // empty array of word sylbs idxs
	~punct_cache = []; // punctuation cache, to keep punctuation at the end of words, before spacebar.
	// it's called when space is pressed, but filled up until then

	~durataLinea = []; // whole line array of durations (cleared with enter key)
	~sideQuest = 0;
	~sillabe_assegnabili = []; // reset assignable syllables
	~lista_indici = []; // reset idxs array

	~used_syl = []; // reset list of encountered syllables (each new one is stored once)
	~assignable_samples = (0..~sounds.size).asSet;
	~saved_pairs = Dictionary.new(~sounds.size); // reset dictionary
	"cached pairs dictionary has been restored to default (empty)".postln;

	"RESET".postln;

	t = TextView(w.asView,Rect(10,10, Window.screenBounds.width/1.5,Window.screenBounds.height-60))
	.focus(true)
	.palette_(QPalette.dark)
	.font_(Font("Courier",25), 5, 10)
	.hasVerticalScroller_(true)
	.background_(Color.grey(1,0));
	// characters allowed in a binary operator: "!@%&*-+=|<>?/".
	// keyboard functions

	t.keyDownAction =
		{ arg view, char, modifiers, unicode, keycode;

			~switch = switch (keycode) // KEY PARSER

			{49} // pressing SPACE BAR
			{
				if(~sideQuest==0 && ~currentLine.isNumber.not, {

					~word_durations = []; // reset array di durate (n elementi, uno per sillaba) -> durata parola
					~indici_parola = [];

					// se c'è solo punteggiatura nella cache, sequenziala prima di processare parole
					// se c'è solo punteggiatura e non parole quando premi spazio, quelle pause vengono aggiunte da sole
					~durataLinea = ~durataLinea++~punct_cache; //aggiungi eventuali rest (punteggiatura) nella cache
					~punct_cache = []; // svuota cache comunque prima di analizzare la parola;

					if( // if there are open and closed brackets, evaluate the expr in there
						t.currentLine.find("(").isNil.not
						&& t.currentLine.find(")").isNil.not,
						{ // create "brackets" array
							~brackets = t.currentLine[(t.currentLine.find("("))..(t.currentLine.find(")"))]
						}, {}
					);

					if( t.currentLine.find("/").isNil
						&& t.currentLine.find("@").isNil // if there's an "end sidequest" mark in the line, DON'T sonify it
						&& t.currentLine.find("").isNil,
						{
							~currentLine = [t.currentLine.asString.toLower
								.reject(_ == $,).reject(_ == $.).reject(_ == $+).reject(_ == $!).reject(_ == $;).reject(_ == $:)
								.reject(_ == $?).reject(_ == $/).reject(_ == $@).reject(_ == $().reject(_ == $)).reject(_ == $+)
								.reject(_ == $|).reject(_ == $>).reject(_ == $=)
								.split($ )].flatten; // dividi linea in parole
						}, {}
					);

					// updata numeroCorrente ogni volta che premi spazio
					t.currentLine.size.do{
						arg i;
						if(
							t.currentLine[i].asString
							.reject(_ == $,).reject(_ == $.).reject(_ == $+).reject(_ == $!).reject(_ == $;).reject(_ == $:)
							.reject(_ == $?).reject(_ == $/).reject(_ == $@).reject(_ == $().reject(_ == $)).reject(_ == $+)
							.reject(_ == $|).reject(_ == $>).reject(_ == $=).reject(_ == $ )
							.interpret.isNumber,
							//&& t.currentLine[i].asString.interpret.isNil.not,
							{~numeroCorrente=t.currentLine[i];
								~currentLine.removeAt(i)
							},
							{"...".postln}
						);
					};
					~currentLine_wordsnum = ~currentLine.flatten.reject(_.isNumber).size; // numero parole nella riga
					~lastWord = ~currentLine[~currentLine.size-1].asString; // prendi l'ultima parola

					if( // se l'ultima parola non è nil, trasformala in sequenza, altrimenti ignorala
						(~lastWord != "nil") && (~lastWord.isNumber.not) && (~lastWord != ($ )),

						{
							// DA QUI SUBENTRA PYPHEN.

							// DIVIDI IN SILLABE.

							~currentWord_syllables = ~get_sylabs.(~language, ~lastWord)
							.reject(_ == $,).reject(_ == $.).reject(_ == $+).reject(_ == $!).reject(_ == $;).reject(_ == $:).reject(_ == $|)
							.reject(_ == $?).reject(_ == $/).reject(_ == $@).reject(_ == $\t ).reject(_.isNumber).reject(_ == $ )/*.split($-)*/;// dividila in sillabe;

							// correct Pyphen string: last syllable always has a fucking additional
							// character at the end (tried deleting it with reject $\n , $_ , $  , $\t , it doesn't work.
							// parse the last syllable and remove the last element)
							~sillaba_incriminata = ~currentWord_syllables[~currentWord_syllables.size-1];
							~sillaba_incriminata.removeAt(~sillaba_incriminata.size-1);

							~currentWord_syllables.postln; // postala sillabazione della parola
							///////////////////////////////////////////////////////////////////////////////////////
							// per ogni sillaba, fai quanto segue
							~currentWord_syllables.size.do{
								arg i;
								var switchMode, passSwitch;

								~modeSelector=~mode;

								~syllable_durations = []; // reset - array di durate (un beat)
								~moltiplica_indice_copie = [];

								// per ogni sillaba (i) segna numero di lettere
								~letters_in_syllable = ~currentWord_syllables.at(i).size; // n lettere in sillaba

							// execute mode_parser for each syllable
							// get a list of idxs and durs based on mode inputted
							m = ~mode_parser.(~mode, i);

							};

							~sillabe_assegnabili = ~sillabe_assegnabili.add(~indici_parola);
							~lista_indici = ~sillabe_assegnabili.flatten;

							// infine, aggiungi parola appena emessa dentro la Linea
							~durataLinea = ~durataLinea.add(~word_durations.flatten).flatten;
							// sequenza simbolica (sillabe -> simboli a scelta)
							//~sequence_type_1 = ~lista_indici.linlin(0, ~unique.size, 0, ~folder.entries.size).round.asInteger;
							~sequence_type_1 = ~lista_indici;
							// (determina quali sample assegnare, da cartella progetto
						},

						// se non Nil, ma
						{
							"__".postln;
						} //se è Nil -->qualcosa ???
					);
					~durataLinea.postln;
					~durataLinea = ~durataLinea++~punct_cache; //aggiungi eventuali rest (punteggiatura) nella cache
				}, { }
				);

			}

			{36} // pressing ENTER
			{


				// update params of interest (why not rate? funziona anche senza?)
				//~stretch = 60/~tempo*~measure;
				~release = ~rel.clip(0.2, 5);
				~attack = ~atk.clip(0.01, 4);
				~tempo = ~bpm;


				if(~lista_indici.isEmpty.not && ~numeroCorrente.isNil.not,
				{
					~sequence_type_1 = ~lista_indici;
					("SEQ "++~numeroCorrente.asString++" : ").post; ~sequence_type_1.postln;
					if( // if rate hasn't been changed, set it as default, otherwise look at param5
						(~param5.isNil) || (~rate.isNil),
						{~rate = 1;
						~param5 = ~rate},//{~rate = ~param5}
					);
				~rate=~param5;
				});



				// AGGIUNGI ULTERIORE SWITCH: SE QUANDO PREMI INVIO C'è UN NUMERO,
				// EXECUTE CORRESPONDING PDEF

				// al premere invio, itera attraverso la riga corrente.
				// se trova numeri, salva l'ultimo in numeroCorrente
				// e attiva lo switch corrispondente

				//~numeroCorrente=numeroCorrente;
				if(~durataLinea.isEmpty.not && ~lista_indici.isEmpty.not,
					{
						~pdefs_choice = switch
						(~numeroCorrente.value.asString.asInteger)

						{0}
						{
							var stretch, durataLinea, sequence_type_1, attack, release, rate, bus;
							stretch=~stretch; durataLinea=~durataLinea; sequence_type_1=~sequence_type_1;
							attack=~attack; release=~release; rate=~rate;
							bus=~bus0;

							Pdef(\playbuf_0, Pbind(
								\instrument, Pfunc{|ev|if (ev.buf.numChannels == 1)
									{\playbuf_test_mono} {\playbuf_test_stereo}},
								\stretch, stretch,
								\dur, Pseq(durataLinea, inf),
								\buf, Pseq(sequence_type_1, inf),
								\rel, Pseq([release], inf),
								\atk, Pseq([attack], inf),
								\rate, Pseq([rate], inf),
								\out, Pseq([bus], inf),
								//\start, Pif(rate<0, Pseq(sequence_type_1, inf).value.numFrames-2, 0),
								//\start, ~sounds[Pkey(\buf).trace].asInteger.numFrames - 2,
							)).play(~clock, quant: stretch);
						}

						{1}
						{
							var stretch, durataLinea, sequence_type_1, attack, release, rate, bus;
							stretch=~stretch; durataLinea=~durataLinea; sequence_type_1=~sequence_type_1;
							attack=~attack; release=~release; rate=~rate;
							bus=~bus1;

							Pdef(\playbuf_1, Pbind(
								\instrument, Pfunc{|ev|if (ev.buf.numChannels == 1)
									{\playbuf_test_mono} {\playbuf_test_stereo}},
								\stretch, stretch,
								\dur, Pseq(durataLinea, inf),
								\buf, Pseq(sequence_type_1, inf),
								\rel, Pseq([release], inf),
								\atk, Pseq([attack], inf),
								\rate, Pseq([rate], inf),
								\out, Pseq([bus], inf),
							)).play(~clock, quant: stretch);
						}
						{2}
						{
							var stretch, durataLinea, sequence_type_1, attack, release, rate, bus;
							stretch=~stretch; durataLinea=~durataLinea; sequence_type_1=~sequence_type_1;
							attack=~attack; release=~release; rate=~rate;
							bus=~bus2;

							Pdef(\playbuf_2, Pbind(
								\instrument, Pfunc{|ev|if (ev.buf.numChannels == 1)
									{\playbuf_test_mono} {\playbuf_test_stereo}},
								\stretch, stretch,
								\dur, Pseq(durataLinea, inf),
								\buf, Pseq(sequence_type_1, inf),
								\rel, Pseq([release], inf),
								\atk, Pseq([attack], inf),
								\rate, Pseq([rate], inf),
								\out, Pseq([bus], inf),
							)).play(~clock, quant: stretch);
						}
						{3}
						{
							var stretch, durataLinea, sequence_type_1, attack, release, rate, bus;
							stretch=~stretch; durataLinea=~durataLinea; sequence_type_1=~sequence_type_1;
							attack=~attack; release=~release; rate=~rate;
							bus=~bus3;

							Pdef(\playbuf_3, Pbind(
								\instrument, Pfunc{|ev|if (ev.buf.numChannels == 1)
									{\playbuf_test_mono} {\playbuf_test_stereo}},
								\stretch, stretch,
								\dur, Pseq(durataLinea, inf),
								\buf, Pseq(sequence_type_1, inf),
								\rel, Pseq([release], inf),
								\atk, Pseq([attack], inf),
								\rate, Pseq([rate], inf),
								\out, Pseq([bus], inf),
							)).play(~clock, quant: stretch);
						}
						{4}
						{
							var stretch, durataLinea, sequence_type_1, attack, release, rate, bus;
							stretch=~stretch; durataLinea=~durataLinea; sequence_type_1=~sequence_type_1;
							attack=~attack; release=~release; rate=~rate;
							bus=~bus4;

							Pdef(\playbuf_4, Pbind(
								\instrument, Pfunc{|ev|if (ev.buf.numChannels == 1)
									{\playbuf_test_mono} {\playbuf_test_stereo}},
								\stretch, stretch,
								\dur, Pseq(durataLinea, inf),
								\buf, Pseq(sequence_type_1, inf),
								\rel, Pseq([release], inf),
								\atk, Pseq([attack], inf),
								\rate, Pseq([rate], inf),
								\out, Pseq([bus], inf),
							)).play(~clock, quant: stretch);
						}
						{5}
						{
							var stretch, durataLinea, sequence_type_1, attack, release, rate, bus;
							stretch=~stretch; durataLinea=~durataLinea; sequence_type_1=~sequence_type_1;
							attack=~attack; release=~release; rate=~rate;
							bus=~bus5;

							Pdef(\playbuf_5, Pbind(
								\instrument, Pfunc{|ev|if (ev.buf.numChannels == 1)
									{\playbuf_test_mono} {\playbuf_test_stereo}},
								\stretch, stretch,
								\dur, Pseq(durataLinea, inf),
								\buf, Pseq(sequence_type_1, inf),
								\rel, Pseq([release], inf),
								\atk, Pseq([attack], inf),
								\rate, Pseq([rate], inf),
								\out, Pseq([bus], inf),
							)).play(~clock, quant: stretch);
						}
						{6}
						{
							var stretch, durataLinea, sequence_type_1, attack, release, rate, bus;
							stretch=~stretch; durataLinea=~durataLinea; sequence_type_1=~sequence_type_1;
							attack=~attack; release=~release; rate=~rate;
							bus=~bus6;

							Pdef(\playbuf_6, Pbind(
								\instrument, Pfunc{|ev|if (ev.buf.numChannels == 1)
									{\playbuf_test_mono} {\playbuf_test_stereo}},
								\stretch, stretch,
								\dur, Pseq(durataLinea, inf),
								\buf, Pseq(sequence_type_1, inf),
								\rel, Pseq([release], inf),
								\atk, Pseq([attack], inf),
								\rate, Pseq([rate], inf),
								\out, Pseq([bus], inf),
							)).play(~clock, quant: stretch);
						}
						{7}
						{
							var stretch, durataLinea, sequence_type_1, attack, release, rate, bus;
							stretch=~stretch; durataLinea=~durataLinea; sequence_type_1=~sequence_type_1;
							attack=~attack; release=~release; rate=~rate;
							bus=~bus7;

							Pdef(\playbuf_7, Pbind(
								\instrument, Pfunc{|ev|if (ev.buf.numChannels == 1)
									{\playbuf_test_mono} {\playbuf_test_stereo}},
								\stretch, stretch,
								\dur, Pseq(durataLinea, inf),
								\buf, Pseq(sequence_type_1, inf),
								\rel, Pseq([release], inf),
								\atk, Pseq([attack], inf),
								\rate, Pseq([rate], inf),
								\out, Pseq([bus], inf),
							)).play(~clock, quant: stretch);
						}
						{8}
						{
							var stretch, durataLinea, sequence_type_1, attack, release, rate, bus;
							stretch=~stretch; durataLinea=~durataLinea; sequence_type_1=~sequence_type_1;
							attack=~attack; release=~release; rate=~rate;
							bus=~bus8;

							Pdef(\playbuf_8, Pbind(
								\instrument, Pfunc{|ev|if (ev.buf.numChannels == 1)
									{\playbuf_test_mono} {\playbuf_test_stereo}},
								\stretch, stretch,
								\dur, Pseq(durataLinea, inf),
								\buf, Pseq(sequence_type_1, inf),
								\rel, Pseq([release], inf),
								\atk, Pseq([attack], inf),
								\rate, Pseq([rate], inf),
								\out, Pseq([bus], inf),
							)).play(~clock, quant: stretch);
						}
						{9}
						{
							var stretch, durataLinea, sequence_type_1, attack, release, rate, bus;
							stretch=~stretch; durataLinea=~durataLinea; sequence_type_1=~sequence_type_1;
							attack=~attack; release=~release; rate=~rate;
							bus=~bus9;

							Pdef(\playbuf_9, Pbind(
								\instrument, Pfunc{|ev|if (ev.buf.numChannels == 1)
									{\playbuf_test_mono} {\playbuf_test_stereo}},
								\stretch, stretch,
								\dur, Pseq(durataLinea, inf),
								\buf, Pseq(sequence_type_1, inf),
								\rel, Pseq([release], inf),
								\atk, Pseq([attack], inf),
								\rate, Pseq([rate], inf),
								\out, Pseq([bus], inf),
							)).play(~clock, quant: stretch);
						};
					},
					{});

				"> > > > EXECUTE "++~numeroCorrente.asString.postln;
				~punct_cache = []; // svuota cache punteggiatura

				~tempo=~bpm;
				~measure=~division;
				~stretch = 60/~tempo*~measure;
				~numSpeakers=(~outs/2)-1;

				~word_durations = []; // reset array di durate (n elementi, uno per sillaba) -> durata parola
				~indici_parola = [];
				~punct_cache = [];
			}

			// PUNTEGGIATURA: aggiunge rests alla cache ~punt_cache. viene concatenata alla linea
			{43} // al premere VIRGOLA / PUNTO E VIRGOLA
			{
				if(modifiers==131072,
					{ // se maiusc, punto e virgola (1/4)
						~punct_cache = ~punct_cache.add(Rest((1/4)*~tempo_factor)).flatten;
					},
					{ // altrimenti è virgola (1/16)
						~punct_cache = ~punct_cache.add(Rest((1/16)*~tempo_factor)).flatten;
					}
				);
			}

			{47} // al premere PUNTO / DUE PUNTI
			{
				if(modifiers==131072,
					{ // se maiusc allora è due punti (1/8)
						~punct_cache = ~punct_cache.add(Rest((1/8)*~tempo_factor)).flatten;
					},
					{ // altrimenti è punto (1)
						~punct_cache = ~punct_cache.add(Rest(1*~tempo_factor)).flatten;
					}
				);
			}




			//{30} // al premere + PLUS) // RESET PHRASE
			{42} // al premere + PLUS) // RESET PHRASE
			{
				~durataLinea = []; // resetta array composito di tutta la riga quando premi invio
				~sillabe_assegnabili = []; // resetta array di sillabe assegnabili
				~lista_indici = []; // resetta array sequenza di simboli
				~sequence_type_1 = [];
				~punct_cache = [];
				"RESET".postln;
			}

			{nil} {"--".postln}


			//{27} // al premere PUNTO INTERROGATIVO ( maiusc + ' )
			{30} // al premere PUNTO INTERROGATIVO ( maiusc + ' )
			{
				if(modifiers == 131072,
					{
						"punto interrogativo".postln;
						~punct_cache = ~punct_cache.add((
							[1/6, Rest(1/6), 1/6, 1/6, Rest(1/6), 1/6]
							.flatten)*~tempo_factor).flatten;
					},

					{ // al premere solo APOSTROFO ( ' ) // devi riuscire a includere l', degl', dell', un', d', gl
						//"apostrofo".postln

					}
				);
			}


			{18} // al premere PUNTO ESCLAMATIVO ( maiusc + 1 )
			{
				if(modifiers == 131072,
					{
						"punto esclamativo".postln;
						~punct_cache = ~punct_cache.add([1/12, 1/12, 1/12]*~tempo_factor).flatten;

					},
					{/* tasto 1*/}

				);
			}

			{50} // al premere tasto > GREATHER THAN (maiusc + <) // PLAY SEQ
			{
				if(modifiers == 131072,
					{
						//p.play; // click sound

						//q.play; // sampler
						//Pdef(\playbuf_pdef).play;
						~pdefs_play = switch
						(~numeroCorrente.value.asString.asInteger)
						{1} {Pdef(\playbuf_1).play(~clock, quant:~stretch); "UNO".postln;}
						{2} {Pdef(\playbuf_2).play(~clock, quant:~stretch); "DUE".postln;}
						{3} {Pdef(\playbuf_3).play(~clock, quant:~stretch); "TRE".postln;}
						{4} {Pdef(\playbuf_4).play(~clock, quant:~stretch); "QUATTRO".postln;}
						{5} {Pdef(\playbuf_5).play(~clock, quant:~stretch); "CINQUE".postln;}
						{6} {Pdef(\playbuf_6).play(~clock, quant:~stretch); "SEI".postln;}
						{7} {Pdef(\playbuf_7).play(~clock, quant:~stretch); "SETTE".postln;}
						{8} {Pdef(\playbuf_8).play(~clock, quant:~stretch); "OTTO".postln;}
						{9} {Pdef(\playbuf_9).play(~clock, quant:~stretch); "NOVE".postln;}
						{0} {Pdef(\playbuf_0).play(~clock, quant:~stretch); "DIECI".postln;};


						"PLAY|".postln;
					},
					{}

				);
			}

			//{10} // al premere tasto | VERTICAL LINE (maiusc + \) // STOP SEQ
			{24} // al premere tasto _ UNDERSCORE (maiusc + -) // STOP
			{
				if(modifiers == 131072,
					{

						// se premi STOP con un NUMERO prima, stoppa
						// Pdef(\playbuf_++NUMERO.asString)
						// altrimenti, by default stoppa NUMERO UNO


						t.currentLine.size.do{
							arg i;
							if( t.currentLine[i].asString
								.reject(_ == $,).reject(_ == $.).reject(_ == $+).reject(_ == $!).reject(_ == $;)
								.reject(_ == $?).reject(_ == $/).reject(_ == $@).reject(_ == $().reject(_ == $))
								.reject(_ == $|).reject(_ == $>).reject(_ == $=).reject(_ == $:).reject(_ == $+)
								.interpret.isNumber
								&& t.currentLine[i].asString.interpret.isNil.not,
								{
									~numeroCorrente = t.currentLine[i];
									~numeroCorrente.postln;
									~pdefs_stop = switch
									(~numeroCorrente.value.asString.asInteger)
									{1} {Pdef(\playbuf_1).stop;}
									{2} {Pdef(\playbuf_2).stop;}
									{3} {Pdef(\playbuf_3).stop;}
									{4} {Pdef(\playbuf_4).stop;}
									{5} {Pdef(\playbuf_5).stop;}
									{6} {Pdef(\playbuf_6).stop;}
									{7} {Pdef(\playbuf_7).stop;}
									{8} {Pdef(\playbuf_8).stop;}
									{9} {Pdef(\playbuf_9).stop;}
									{0} {Pdef(\playbuf_0).stop;};
									~numeroCorrente= nil;
								},
								{
									~numeroCorrente = nil;
									Pdef(\playbuf_1).stop;
									Pdef(\playbuf_2).stop;
									Pdef(\playbuf_3).stop;
									Pdef(\playbuf_4).stop;
									Pdef(\playbuf_5).stop;
									Pdef(\playbuf_6).stop;
									Pdef(\playbuf_7).stop;
									Pdef(\playbuf_8).stop;
									Pdef(\playbuf_9).stop;
									Pdef(\playbuf_0).stop;
								}
							);
						};
						"STOP|".postln;
					},
				);
			}

			{26} // al premere il tasto / (SLASH) (maiusc+7) // CLOSE EXPRESSION
			{
				if(modifiers == 131072,
					{
						var compile;

						~sideQuest = 0;
						~durataLinea = []; // resetta array composito di tutta la riga quando premi invio
						~sillabe_assegnabili = []; // resetta array di sillabe assegnabili
						~lista_indici = []; // resetta array sequenza di simboli
						~sequence_type_1 = [];
						~punct_cache = [];
						~currentLine = [];
						"close expression".postln;
						// ~command is the current command to be compiled
						~command = t.currentLine.reject(_ == $ ).reject(_ == $/).replace("@", "~"); // ripulisci tutto ciò tra @ e / e rimuovi estremi
						//~command = ~command++";";
						if(~command.isNil.not && ~command.asString != "|", {compile=~command.interpret}, {"nil.".postln});
						//compile = ~command.interpret;

						// evaluate these side quests after closing expression (/)

						// qui mappa i mini-comandi della side quest con le variabili globali
						// che controllano parametri musicali (lista in aggiornamento)
						/////////////////////////////////////////////////////////
						// PARAMETER 1: MODE SELECTION
						// change between mode 0 and 1. updated after +
						//~param1 = ~mode_parser.(~mode).do(_.postln);
						~param1 = ~mode;

						///////////////////////////////////////////////////
						// PARAMETER 2: FATTORE TEMPO (0-2)
						~param2 = ~tFact;
						~tempo_factor = ~param2.clip(0.1,4);

						"fattore tempo: "++~tempo_factor.postln;
						////////////////////////////////////////////////////
						// PARAMETER 3: RELEASE BUF PLAYER (0.2 - 5)
						~param3 = ~rel;
						~release = ~rel.clip(0.2, 5);

						"release: "++~release.asString.postln;
						////////////////////////////////////////////////////
						// PARAMETER 4: ATK BUF PLAYER (0.01 - 4)
						~param4 = ~atk;
						~attack = ~atk.clip(0.01, 4);

						"attack: "++~attack.asString.postln;

						////////////////////////////////////////////////////
						// PARAMETER 5: RATE [-12, 12; default 1, reverse -1]

						~param5 = ~rate;

						"rate: "++~rate.asString.postln;

						////////////////////////////////////////////////////
						// PARAMETER 6: TEMPO (10..500); default 120
						~param6 = ~bpm;
						~tempo = ~param6;

						"tempo: "++~tempo.asString.postln;

						////////////////////////////////////////////////////
						// PARAMETER 7: MEASURE (1..16); default 4

						~param7 = ~division;
						~measure = ~param7;
						/////////////////////////////////////
						~stretch = (60/~tempo)*~measure;

						"measure: "++~measure.asString.postln;
						////////////////////////////////////////////////////
						// PARAMETER 8: LANGUAGE ["it", "en", "es", "is" etc.]; default "it"

						~param8 = ~lang;
						~language = ~param8;
						/////////////////////////////////////////////////
						// PARAMETER 9: NUMBER OF SPEAKER OUTPUTS (FOR SMEKKLEYSA 4-SPEAKERS SETUP)
						// remember to boot the server with numChannels = 4;
						// in SillyCode, outs can be 2 (default) or 4.
						~param9 = ~outs;
						~numSpeakers = (~param9/2)-1;


					},
					{}
				);
			}

			//{41} // al premere tasto @ (alt + ò) // OPEN EXPRESSION
			{12} // al premere tasto @ (alt + ò) // OPEN EXPRESSION
			{
				if(modifiers == 524288,
					{
						~sideQuest = 1;
						"open expression".postln;
					},
					//{"no".postln}

				);
			}

			{} {};
			// fine azioni assegnate


			if(
				~sideQuest == 1,
				{"still side questin dude".postln},
				{if(~sideQuest == 0, {"no side questn"})}

			);



		};

	// quando ci sono più di n righe, cancella tutto

	w.front;
};


defer{
fork{
	1.do{
		1.wait;
		~aa.();
		1.wait;
		~bb.();
		3.wait;
		~cc.();
		/*5.wait;
		~dd.();
		if(Server.allBootedServers.isEmpty.not,
			{~dd.();},
			{":( could not initialise SillyCode. try executing the program again"}
		);*/
	}
};
}
)

////////////////////////////////////////////////////////////////////////////////////////////
