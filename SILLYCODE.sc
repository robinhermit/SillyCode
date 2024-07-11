Server.default.options.device = "Built-in Output";
Server.default.options.device = "BlackHole 2ch";
Server.default.options.device = "Scarlett 18i20 USB";
s.reboot;
Server.killAll;
SerialPort.closeAll;

(
~aa = {
	// give it a few seconds to process this

	~get_sylabs = {|language, input|
		format(
			// check python path! otherwise unixCmdGetStdOut won't work
			"/Library/Frameworks/Python.framework/Versions/3.11/bin/python3.11 -c \"import pyphen; out = pyphen.Pyphen(left=1, right=1, lang='%').inserted('%'); print(out)\"",
			language, input
		).unixCmdGetStdOut.split($-)
	};

	"Parsing function: ready.".postln;

	// fake stringa di test
	~stringa = "Questo testo qua, come esempio";
	// inizializza con italiano
	~language= "it";

	"SillyCode - a live-coding environment for text sonification".postln;

	Server.killAll;
};

//////////////////////

~bb = {
	// ready to go! now you only have to insert a word here as string. if it doesn't exist, it'll print nil, otherwise it'll print the correct hyphenation
	// test function
	x = ~get_sylabs.(~language, ~stringa).do(_.postln);
	if(x.isNil.not, {">>>>> 100 % >>>>>>>>>>>> Association confirmed".postln;
		x.postln;
	});
};

~cc = {

	// server operations

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

		// DON'T USE LETTERS: THEYRE GONNA BE INTERPRETED AS NUMBERS IN SILLYCODE!!!!!
		// REPLACE f AND h WITH SOMETHING LIKE ~frameSize and ~hopSize
		~fft_frameSize = 512; // fft analysis frame size
		~fft_hopSize = 0.25; // hop size

		Buffer.freeAll;
		~sounds = Array.new;
		// folder = PathName.new("/Users/Robin/Desktop/TUTTO/ATTIVI/SillyCode/Samples/");
		folder = PathName.new(thisProcess.nowExecutingPath.dirname+/+"Sounds");
		// need to increase max buf number (1024)
		//~folderEntries
		folder.entries.scramble.do({
			arg path;
			var soundfile, framerate, hopsize;

			soundfile = SoundFile.new(path.fullPath); // current soundfile to calculate framesize duration
			framerate = ~fft_frameSize;
			hopsize = ~fft_hopSize;

/*			~fft_data = ~fft_data.add(Buffer.alloc(s,
				soundfile.duration.calcPVRecSize(
					~fft_frameSize,~fft_hopSize
			))); // buffers allocated to store FFT data*/

			~sounds = ~sounds.add(Buffer.readChannel(s, path.fullPath, channels: [0]));
		});


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
		//~rear_bus = Bus.audio(s,2);
		~fs_buf = Bus.audio(s,4);
		~grain_buf = Bus.audio(s,4);
		~rev_bus = Bus.audio(s,4);

		~group1 = Group.new; // group for loops 0,1,2
		~group2 = Group.new(~group1, \addAfter); // group for loops 3,4,5
		~group3 = Group.new(~group2, \addAfter); // group for loops 6,7,8,9
/*
		SynthDef(\click, {
			arg freq=100;
			var sig;
			sig = Ringz.ar(Impulse.ar(0.1), [freq, freq+1], 0.1) * Line.ar(1,0,0.1,1,0,2);
			sig = LPF.ar(sig, 150);
			Out.ar(0, sig);
		}).add;
*/
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

		// analyse and store samples in fft buffers

/*		SynthDef.new(\pv_rec, {// RECBUF IS THE DESTINATION BUFFER (in ~fft_data collection)
			arg recBuf=1, soundBufnum=2,  // SOUND TO ANALYSE (each in ~sounds)
			framesize = ~fft_frameSize, hopsize = ~fft_hopSize;
			var input, chain, bufnum;
			bufnum = LocalBuf.new(framesize);
			Line.kr(1,1,BufDur.kr(soundBufnum), doneAction:2);
			input = PlayBuf.ar(1, soundBufnum, BufRateScale.kr(soundBufnum), loop:0);
			chain = FFT(bufnum, input, hopsize, 1);
			chain = PV_RecordBuf(chain, recBuf, 0,1,0,hopsize,1);
		}).add;

		// FFT analysis of all samples
		fork{
			~sounds.do{
				arg item, i;
				var currentDestination, currentSource;
				currentDestination = ~fft_data[i];
				currentSource = ~sounds[i];
				Synth(\pv_rec, [\recBuf, currentDestination.bufnum, \soundBufnum, currentSource.bufnum], addAction: 'addToTail');
				s.sync;
				// takes some time to free all the synths (5 min)
			}
		}*/
		// this works but takes a bunch
		// continue adding PV_BufRd (con un pointer) o PV_PlayBuf (come sopra ma no freeze)

	};

	"server booted!".postln;
};


////////////////////////////////////////////////////////////////////////////////////////////////////////
// G U I   F U N C T I O N S //////////////////////////////////////
/////////////////////////////////////////////////7

~dd = {

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



/*
	~port = SerialPort.new("/dev/tty.usbserial-A703Y978", 115200);

	Tdef(\readSerial, {
		loop{
			var byte, str, res;
			if(~port.read==10,
				{	str = "";
					while(
						{byte = ~port.read; byte!=13},
						{str = str++byte.asAscii}
					);
					res = str.split($ );
					~res=res;
			});
		}
	}).play;

	//
	// if( // if Arduino is connected, create serial port and start reading.
	// 	// otherwise just deactivate the visualiser with a decoy array
	// 	~port.isNil.not, {
	//
	// 		~port = SerialPort.new("/dev/tty.usbserial-A703Y978", 115200);
	//
	// 		Tdef(\readSerial, {
	// 			loop{
	// 				var byte, str, res;
	// 				if(~port.read==10,
	// 					{	str = "";
	// 						while(
	// 							{byte = ~port.read; byte!=13},
	// 							{str = str++byte.asAscii}
	// 						);
	// 						res = str.split($ );
	// 						~res=res;
	// 				});
	// 			}
	// 		}).play;
	//
	// 	}, {
	// 		~res = Array.fill(rows*columns, range_max);
	// 	}
	// );
*/


	if( // SALVATAGGIO AUTOMATICO DEL TESTO SE C'è QUALCOSA DI SCRITTO
		t.notNil,
		{
			var file,date;
			date = Date.getDate.format("%Y%m%d_%H%M");
			file = File.new(thisProcess.nowExecutingPath.dirname+/+"Saved"+/+"SillyCode_sweater"++date++".txt", "w");
			file.write(t.string.asString);
			file.close;
			"testo salvato".postln;
		},
		{"testo vuoto".postln}
	);



	// resetta parametri temporali, crea tempoclock
	~tempo = 120; ~bpm=120;
	~measure = 4; ~division=4;
	~stretch = (60/~tempo)*~measure;

	// resetta numero speakers (4 outs are always open, but usually not used unless
	// the side quest @outs=4/
	~numSpeakers = 0; ~outs = 2;

	//
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
	~fattore_tempo = 1; ~tFact=1;
	~modeSelector = 0; ~mode=0;
	/*
	~tempo = 120; ~bpm=120;
	~measure = 4; ~division=4;
	~stretch = (60/~tempo)*~measure;
	*/
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
	z = PdefAllGui(11);
	s.meter;

/*

	// creates a series of columns (background black)
	views = (0..14).collect{
		arg m, ind;
		CompositeView(w, Rect(10+(ind*scale), 10, width: w.bounds.width/columns+55, height: w.bounds.height-60))
		.background_(Color.gray(0))
	};
	// all views in a composite view
	views_full = views.collect{
		arg o, index;
		rows.collect{
			arg p;
			CompositeView(views[index], Rect(0,0+(p*scale), scale, scale))
			.background_(Color.gray(0.5)) // <-- this is the value to modulate
		}
	};

	~subRes = Array.new(200);

	~res.do({
		arg item, i;
		if(
			(item.asInteger) < 1000,
			{
				~subRes.add([i, item])
			}
		);
	});  // 32 channels?
	// 12 front
	// 9 back
	// 12 right
	// 8 left

	Tdef(\sweater, {
		loop{

			~subRes = Array.new(200);

			~res.do({
				arg item, i;
				if(
					(item.asInteger) < 900,
					{
						~subRes.add([i, item])
					}
				);
			});  // 32 channels?
			// 12 front
			// 9 back
			// 12 right
			// 8 left
			~sect1 = ~subRes[0..(~subRes.size/3).round.asInteger];
			~sect2 = ~subRes[(((~subRes.size/3).round.asInteger)+1)..((~subRes.size/3)+(~subRes.size/4)).round.asInteger];
			~sect3 = ~subRes[(((~subRes.size/3)+(~subRes.size/4)).round.asInteger+1)..((~subRes.size/3)+(~subRes.size/4)+(~subRes.size/6)).round.asInteger];
			~sect4 = ~subRes[(((~subRes.size/3)+(~subRes.size/4)+(~subRes.size/6)).round.asInteger+1)..((~subRes.size/3)+(~subRes.size/4)+(~subRes.size/6)+(~subRes.size/6)).round.asInteger];

			if(
				((~subRes.size/3)+(~subRes.size/4)+(~subRes.size/6)+(~subRes.size/7)).round.asInteger < ~subRes.size,
				{~sec_remain = ~subRes[((~subRes.size/3)+(~subRes.size/4)+(~subRes.size/6)+(~subRes.size/7)).round.asInteger+1..~subRes.size-1]},
				{~sec_remain=nil}
			);
			0.1.wait;
	}}).play(AppClock);

	// this task updates the GUI at 20ms rate (slower than arduino readings)
	Tdef(\colorControl, {
		loop{

			var colScope, tempRevAvg1, tempRevAvg2, tempFsAvg,
			col_L, col_R, col_l, col_r,
			speaker1, speaker2, speaker3, speaker4,
			sp1_avg, sp2_avg, sp3_avg, sp4_avg,
			grain_duration, ratescale,
			sweater_scale;
			var coor_coll = [];
			~coor_coll = nil;

			// FXs CONTROL
			// sort res[] columns from matrix and parse values to playbuf friendly array

			// rev controls
			middleRow = ((rows* (columns/2).round) - rows).asInteger; // first index of middle row
			threeQuarters = (~res.size)-(5*rows).asFloat; // first index of basically row 10 or smt



/*			rowStart = index*rows;
			rowEnd = (index*rows)+(rows-1);
			// raw values coming from arduino, in the current chunk
			scope = ~res[rowStart..rowEnd];*/

			// define pan columns (L and R = main; l and r = rear)
			// on the matrix
			col_L = ~res[(rows*4)..(rows*4)+9];
			col_R = ~res[((rows*12)+1)..((rows*12)+10)];
			col_l = ~res[(rows*5)..((rows*5)+9)];
			col_r = ~res[((rows*13)+1)..((rows*13)+10)];

			if(~numSpeakers == 0 ,
				{ // se outs è uguale a 2,
					// default stereo settings
					speaker1 = Array.fill(10, 0.5);
					sp1_avg = speaker1;
					speaker2 = Array.fill(10, 0.5);
					sp2_avg = speaker2;
					speaker3 = Array.fill(10, 0);
					sp3_avg = sp3_avg;
					speaker4 = Array.fill(10, 0);
					sp4_avg = speaker4;

				}, {

					// se outs diverso da stereo,  (deve essere 4..)
					speaker1 = col_L.collect{
							arg item, i;
							item.asFloat.linlin(range_min, range_max, 0.70, 0);
					};

					sp1_avg = speaker1.sum / rows;

					speaker2 = col_R.collect{
							arg item, i;
							item.asFloat.linlin(range_min, range_max, 0.70, 0);
					};

					sp2_avg = speaker2.sum / rows;

					speaker3 = col_l.collect{
							arg item, i;
							item.asFloat.linlin(range_min, range_max, 0.70, 0);
					};

					sp3_avg = speaker3.sum / rows;

					speaker4 =
						col_r.collect{
							arg item, i;
							item.asFloat.linlin(range_min, range_max, 0.70, 0);
					};


					sp4_avg = speaker4.sum / rows;

				}

			);

			~sect1_loaded = Array.new(20);
			~sect1.do{
				arg item, i;
				~sect1_loaded.add(item[1]);
			};
			~sect1_loaded = ~sect1_loaded++Array.fill(10-~sect1.size, range_max);

			~sect2_loaded = Array.new(20);
			~sect2.do{
				arg item, i;
				~sect2_loaded.add(item[1]);
			};
			~sect2_loaded = ~sect2_loaded++Array.fill(10-~sect2.size, range_max);

			~sect3_loaded = Array.new(20);
			~sect3.do{
				arg item, i;
				~sect3_loaded.add(item[1]);
			};
			~sect3_loaded = ~sect3_loaded++Array.fill(10-~sect3.size, range_max);

			~sect4_loaded = Array.new(20);
			~sect4.do{
				arg item, i;
				~sect4_loaded.add(item[1]);
			};
			~sect4_loaded = ~sect4_loaded++Array.fill(10-~sect4.size, range_max);



			// set grainamt by groups
			if(
				(~sec_remain.isNil.not)&&(~sec_remain.size >= 3),
				{

					~group1.set(\grainAmt, ~sec_remain[0][1].asFloat.linlin(range_min,range_max, 1,0));
					~group2.set(\grainAmt, ~sec_remain[1][1].asFloat.linlin(range_min,range_max, 1,0));
					~group3.set(\grainAmt, ~sec_remain[2][1].asFloat.linlin(range_min,range_max, 1,0));

					~ch0.set(
						\lowcut, ~res[0].asFloat.linlin(range_min, range_max, 2000,20),
						\hicut, ~sect2_loaded[0].asFloat.linlin(range_min, range_max, 2100,15000),
						\fsAmt, ~res[threeQuarters].asFloat.linlin(range_min, range_max, 1,0),
						\crushAmt, ~sect3_loaded[0].asFloat.linlin(range_min, range_max, 0.5,0),
						\revAmt, ~sect1_loaded[0].asFloat.linlin(range_min, range_max, 0.8,0),
						\dryAmt, ~sect1_loaded[0].asFloat.linlin(range_min, range_max, 0,1),
						\sp1_amt, speaker1[0],
						\sp2_amt, speaker2[0],
					);

					~ch1.set(
						\lowcut, ~res[1].asFloat.linlin(range_min, range_max, 2000,20),
						\hicut,~sect2_loaded[1].asFloat.linlin(range_min, range_max, 2100,15000),
						\fsAmt, ~res[threeQuarters+1].asFloat.linlin(range_min, range_max, 1,0),
						\crushAmt, ~sect3_loaded[1].asFloat.linlin(range_min, range_max, 0.5,0),
						\revAmt, ~sect1_loaded[1].asFloat.linlin(range_min, range_max, 0.8,0),
						\dryAmt, ~sect1_loaded[1].asFloat.linlin(range_min, range_max, 0,1),
						\sp1_amt, speaker1[1],
						\sp2_amt, speaker2[1],
					);

					~ch2.set(
						\lowcut, ~res[2].asFloat.linlin(range_min, range_max, 2000,20),
						\hicut, ~sect2_loaded[2].asFloat.linlin(range_min, range_max, 2100,15000),
						\fsAmt, ~res[threeQuarters+2].asFloat.linlin(range_min, range_max, 1,0),
						\crushAmt, ~sect3_loaded[2].asFloat.linlin(range_min, range_max, 0.5,0),
						\revAmt, ~sect1_loaded[2].asFloat.linlin(range_min, range_max, 0.8,0),
						\dryAmt, ~sect1_loaded[2].asFloat.linlin(range_min, range_max, 0,1),
						\sp1_amt, speaker1[2],
						\sp2_amt, speaker2[2],
					);

					~ch3.set(
						\lowcut, ~res[3].asFloat.linlin(range_min, range_max, 2000,20),
						\hicut, ~sect2_loaded[3].asFloat.linlin(range_min, range_max, 2100,15000),
						\fsAmt, ~res[threeQuarters+3].asFloat.linlin(range_min, range_max, 1,0),
						\crushAmt, ~sect3_loaded[3].asFloat.linlin(range_min, range_max, 0.5,0),
						\revAmt, ~sect1_loaded[3].asFloat.linlin(range_min, range_max, 0.8,0),
						\dryAmt, ~sect1_loaded[3].asFloat.linlin(range_min, range_max, 0,1),
						\sp1_amt, speaker1[3],
						\sp2_amt, speaker2[3],
					);

					~ch4.set(
						\lowcut, ~res[4].asFloat.linlin(range_min, range_max, 2000,20),
						\hicut, ~sect2_loaded[4].asFloat.linlin(range_min, range_max, 2100,15000),
						\fsAmt, ~res[threeQuarters+4].asFloat.linlin(range_min, range_max, 1,0),
						\crushAmt, ~sect3_loaded[4].asFloat.linlin(range_min, range_max, 0.5,0),
						\revAmt, ~sect1_loaded[4].asFloat.linlin(range_min, range_max, 0.8,0),
						\dryAmt, ~sect1_loaded[4].asFloat.linlin(range_min, range_max, 0,1),
						\sp1_amt, speaker1[4],
						\sp2_amt, speaker2[4],
					);

					~ch5.set(
						\lowcut, ~res[5].asFloat.linlin(range_min, range_max, 2000,20),
						\hicut, ~sect2_loaded[5].asFloat.linlin(range_min, range_max, 2100,15000),
						\fsAmt, ~res[threeQuarters+5].asFloat.linlin(range_min, range_max, 1,0),
						\crushAmt, ~sect4_loaded[0].asFloat.linlin(range_min, range_max, 0.5,0),
						\revAmt, ~sect1_loaded[5].asFloat.linlin(range_min, range_max, 0.8,0),
						\dryAmt, ~sect1_loaded[5].asFloat.linlin(range_min, range_max, 0,1),
						\sp1_amt, speaker1[5],
						\sp2_amt, speaker2[5],
					);

					~ch6.set(
						\lowcut, ~res[6].asFloat.linlin(range_min, range_max, 2000,20),
						\hicut, ~sect2_loaded[6].asFloat.linlin(range_min, range_max, 2100,15000),
						\fsAmt, ~res[threeQuarters+6].asFloat.linlin(range_min, range_max, 1,0),
						\crushAmt, ~sect4_loaded[1].asFloat.linlin(range_min, range_max, 0.5,0),
						\revAmt, ~sect1_loaded[6].asFloat.linlin(range_min, range_max, 0.8,0),
						\dryAmt, ~sect1_loaded[6].asFloat.linlin(range_min, range_max, 0,1),
						\sp1_amt, speaker1[6],
						\sp2_amt, speaker2[6],
					);

					~ch7.set(
						\lowcut, ~res[7].asFloat.linlin(range_min, range_max, 2000,20),
						\hicut, ~sect2_loaded[7].asFloat.linlin(range_min, range_max, 2100,15000),
						\fsAmt, ~res[threeQuarters+7].asFloat.linlin(range_min, range_max, 1,0),
						\crushAmt, ~sect4_loaded[2].asFloat.linlin(range_min, range_max, 0.5,0),
						\revAmt, ~sect1_loaded[7].asFloat.linlin(range_min, range_max, 0.8,0),
						\dryAmt, ~sect1_loaded[7].asFloat.linlin(range_min, range_max, 0,1),
						\sp1_amt, speaker1[7],
						\sp2_amt, speaker2[7],
					);

					~ch8.set(
						\lowcut, ~res[8].asFloat.linlin(range_min, range_max, 2000,20),
						\hicut, ~sect2_loaded[8].asFloat.linlin(range_min, range_max, 2100,15000),
						\fsAmt, ~res[threeQuarters+8].asFloat.linlin(range_min, range_max, 1,0),
						\crushAmt, ~sect4_loaded[3].asFloat.linlin(range_min, range_max, 0.5,0),
						\revAmt, ~sect1_loaded[8].asFloat.linlin(range_min, range_max, 0.8,0),
						\dryAmt, ~sect1_loaded[8].asFloat.linlin(range_min, range_max, 0,1),
						\sp1_amt, speaker1[8],
						\sp2_amt, speaker2[8],
					);

					~ch9.set(
						\lowcut, ~res[9].asFloat.linlin(range_min, range_max, 2000,20),
						\hicut, ~sect2_loaded[9].asFloat.linlin(range_min, range_max, 2100,15000),
						\fsAmt, ~res[threeQuarters+9].asFloat.linlin(range_min, range_max, 1,0),
						\crushAmt,~sect4_loaded[4].asFloat.linlin(range_min, range_max, 0.5,0),
						\revAmt, ~sect1_loaded[9].asFloat.linlin(range_min, range_max, 0.8,0),
						\dryAmt, ~sect1_loaded[9].asFloat.linlin(range_min, range_max, 0,1),
						\sp1_amt, speaker1[9],
						\sp2_amt, speaker2[9],
					);
				},
				{
					~group1.set(\grainAmt, ~res[(rows*8)-1].asFloat.linlin(range_min,range_max, 1,0));
					~group2.set(\grainAmt, ~res[(rows*7)-2].asFloat.linlin(range_min,range_max, 1,0));
					~group3.set(\grainAmt, ~res[(rows*4)-5].asFloat.linlin(range_min,range_max, 1,0));

					~ch0.set(
						\lowcut, ~res[0].asFloat.linlin(range_min, range_max, 2000,20),
						\hicut, ~res[(~res.size-rows)].asFloat.linlin(range_min, range_max, 2100,15000),
						\fsAmt, ~res[threeQuarters].asFloat.linlin(range_min, range_max, 1,0),
						\crushAmt, ~res[rows*2].asFloat.linlin(range_min, range_max, 0.5,0),
						\revAmt, ~res[middleRow].asFloat.linlin(range_min, range_max, 0.8,0),
						\dryAmt, ~res[middleRow].asFloat.linlin(range_min, range_max, 0,1),
						\sp1_amt, speaker1[0],
						\sp2_amt, speaker2[0],
					);

					~ch1.set(
						\lowcut, ~res[1].asFloat.linlin(range_min, range_max, 2000,20),
						\hicut,~res[(~res.size-rows)+1].asFloat.linlin(range_min, range_max, 2100,15000),
						\fsAmt, ~res[threeQuarters+1].asFloat.linlin(range_min, range_max, 1,0),
						\crushAmt, ~res[(rows*2)+1].asFloat.linlin(range_min, range_max, 0.5,0),
						\revAmt, ~res[middleRow+1].asFloat.linlin(range_min, range_max, 0.8,0),
						\dryAmt, ~res[middleRow+1].asFloat.linlin(range_min, range_max, 0,1),
						\sp1_amt, speaker1[1],
						\sp2_amt, speaker2[1],
					);

					~ch2.set(
						\lowcut, ~res[2].asFloat.linlin(range_min, range_max, 2000,20),
						\hicut, ~res[(~res.size-rows)+2].asFloat.linlin(range_min, range_max, 2100,15000),
						\fsAmt, ~res[threeQuarters+2].asFloat.linlin(range_min, range_max, 1,0),
						\crushAmt, ~res[(rows*2)+2].asFloat.linlin(range_min, range_max, 0.5,0),
						\revAmt, ~res[middleRow+2].asFloat.linlin(range_min, range_max, 0.8,0),
						\dryAmt, ~res[middleRow+2].asFloat.linlin(range_min, range_max, 0,1),
						\sp1_amt, speaker1[2],
						\sp2_amt, speaker2[2],
					);

					~ch3.set(
						\lowcut, ~res[3].asFloat.linlin(range_min, range_max, 2000,20),
						\hicut, ~res[(~res.size-rows)+3].asFloat.linlin(range_min, range_max, 2100,15000),
						\fsAmt, ~res[threeQuarters+3].asFloat.linlin(range_min, range_max, 1,0),
						\crushAmt, ~res[(rows*2)+3].asFloat.linlin(range_min, range_max, 0.5,0),
						\revAmt, ~res[middleRow+3].asFloat.linlin(range_min, range_max, 0.8,0),
						\dryAmt, ~res[middleRow+3].asFloat.linlin(range_min, range_max, 0,1),
						\sp1_amt, speaker1[3],
						\sp2_amt, speaker2[3],
					);

					~ch4.set(
						\lowcut, ~res[4].asFloat.linlin(range_min, range_max, 2000,20),
						\hicut, ~res[(~res.size-rows)+4].asFloat.linlin(range_min, range_max, 2100,15000),
						\fsAmt, ~res[threeQuarters+4].asFloat.linlin(range_min, range_max, 1,0),
						\crushAmt, ~res[(rows*2)+4].asFloat.linlin(range_min, range_max, 0.5,0),
						\revAmt, ~res[middleRow+4].asFloat.linlin(range_min, range_max, 0.8,0),
						\dryAmt, ~res[middleRow+4].asFloat.linlin(range_min, range_max, 0,1),
						\sp1_amt, speaker1[4],
						\sp2_amt, speaker2[4],
					);

					~ch5.set(
						\lowcut, ~res[5].asFloat.linlin(range_min, range_max, 2000,20),
						\hicut, ~res[(~res.size-rows)+5].asFloat.linlin(range_min, range_max, 2100,15000),
						\fsAmt, ~res[threeQuarters+5].asFloat.linlin(range_min, range_max, 1,0),
						\crushAmt, ~res[(rows*2)+5].asFloat.linlin(range_min, range_max, 0.5,0),
						\revAmt, ~res[middleRow+5].asFloat.linlin(range_min, range_max, 0.8,0),
						\dryAmt, ~res[middleRow+5].asFloat.linlin(range_min, range_max, 0,1),
						\sp1_amt, speaker1[5],
						\sp2_amt, speaker2[5],
					);

					~ch6.set(
						\lowcut, ~res[6].asFloat.linlin(range_min, range_max, 2000,20),
						\hicut, ~res[(~res.size-rows)+6].asFloat.linlin(range_min, range_max, 2100,15000),
						\fsAmt, ~res[threeQuarters+6].asFloat.linlin(range_min, range_max, 1,0),
						\crushAmt, ~res[(rows*2)+6].asFloat.linlin(range_min, range_max, 0.5,0),
						\revAmt, ~res[middleRow+6].asFloat.linlin(range_min, range_max, 0.8,0),
						\dryAmt, ~res[middleRow+6].asFloat.linlin(range_min, range_max, 0,1),
						\sp1_amt, speaker1[6],
						\sp2_amt, speaker2[6],
					);

					~ch7.set(
						\lowcut, ~res[7].asFloat.linlin(range_min, range_max, 2000,20),
						\hicut, ~res[(~res.size-rows)+7].asFloat.linlin(range_min, range_max, 2100,15000),
						\fsAmt, ~res[threeQuarters+7].asFloat.linlin(range_min, range_max, 1,0),
						\crushAmt, ~res[(rows*2)+7].asFloat.linlin(range_min, range_max, 0.5,0),
						\revAmt, ~res[middleRow+7].asFloat.linlin(range_min, range_max, 0.8,0),
						\dryAmt, ~res[middleRow+7].asFloat.linlin(range_min, range_max, 0,1),
						\sp1_amt, speaker1[7],
						\sp2_amt, speaker2[7],
					);

					~ch8.set(
						\lowcut, ~res[8].asFloat.linlin(range_min, range_max, 2000,20),
						\hicut, ~res[(~res.size-rows)+8].asFloat.linlin(range_min, range_max, 2100,15000),
						\fsAmt, ~res[threeQuarters+8].asFloat.linlin(range_min, range_max, 1,0),
						\crushAmt, ~res[(rows*2)+8].asFloat.linlin(range_min, range_max, 0.5,0),
						\revAmt, ~res[middleRow+8].asFloat.linlin(range_min, range_max, 0.8,0),
						\dryAmt, ~res[middleRow+8].asFloat.linlin(range_min, range_max, 0,1),
						\sp1_amt, speaker1[8],
						\sp2_amt, speaker2[8],
					);

					~ch9.set(
						\lowcut, ~res[9].asFloat.linlin(range_min, range_max, 2000,20),
						\hicut, ~res[(~res.size-rows)+9].asFloat.linlin(range_min, range_max, 2100,15000),
						\fsAmt, ~res[threeQuarters+9].asFloat.linlin(range_min, range_max, 1,0),
						\crushAmt, ~res[(rows*2)+9].asFloat.linlin(range_min, range_max, 0.5,0),
						\revAmt, ~res[middleRow+9].asFloat.linlin(range_min, range_max, 0.8,0),
						\dryAmt, ~res[middleRow+9].asFloat.linlin(range_min, range_max, 0,1),
						\sp1_amt, speaker1[9],
						\sp2_amt, speaker2[9],
					);
				}
			);

			// some operations to calculate the averatge of sme of the columns

			// frequency shifter param (fshift)
			tempFsAvg = ~sect4_loaded.sum{arg i; i.asFloat};
			tempFsAvg = tempFsAvg / ~sect4_loaded.size;
			tempFsAvg = tempFsAvg.linlin(range_min, range_max, 200, 0.4);

			// rev params (room size / decay)
			// room size
			tempRevAvg1 = ~sect1_loaded.sum{arg i; i.asFloat};
			tempRevAvg1 = tempRevAvg1 / ~sect1_loaded.size;
			tempRevAvg1 = tempRevAvg1.linlin(range_min, range_max, 5, 80);

			tempRevAvg2 = ~sect2_loaded.sum{arg i; i.asFloat};
			tempRevAvg2 = tempRevAvg2 / ~sect2_loaded.size;
			tempRevAvg2 = tempRevAvg2.linlin(range_min, range_max, 1.3, 5);


			// set the computed averages on their fx synths
			~freqshift_ch.set(
				\fshift, tempFsAvg,
				\sp1_amt, sp1_avg,
				\sp2_amt, sp2_avg,
				\sp3_amt, sp3_avg,
				\sp4_amt, sp4_avg,
			);

			~rev_ch.set(
				\roomSize, tempRevAvg1, // default is 80
				\revTime, tempRevAvg2, // default is 5
				\sp1_amt, sp1_avg,
				\sp2_amt, sp2_avg,
				\sp3_amt, sp3_avg,
				\sp4_amt, sp4_avg,
			);

			grain_duration = ~res[10..12].collect{
				arg item, i;
				item.asFloat.linlin(range_min, range_max, 1, 0.05);
			};
			grain_duration = grain_duration.sum / 3;

			ratescale = ~res[23..25].collect{
				arg item, i;
				item.asFloat.linlin(range_min, range_max, 0.1, 1);
			};

			~grain_ch.set(
				\duration, grain_duration,
				\ratescale, ratescale,
				\sp1_amt, sp1_avg,
				\sp2_amt, sp2_avg,
				\sp3_amt, sp3_avg,
				\sp4_amt, sp4_avg,
			);


			views_full.do{
				arg view, index;
				var rowStart, rowEnd,
				shades, store_index, scope,
				coordinates, coor_coll;

				// fragments ~res in as many chunks as rows, to place it in views
				rowStart = index*rows;
				rowEnd = (index*rows)+(rows-1);
				// raw values coming from arduino, in the current chunk
				scope = ~res[rowStart..rowEnd];

				// map raw values with color intensity
				shades = scope.collect{
					arg j;
					j.asFloat.linlin(range_min,range_max,1,0);
				};

				// store index to use in nested scopes
				store_index = index;



				// check for points exceeding specified threshold
				coordinates = scope.collect{
					arg k, idx;

					if(k.asFloat.linlin(range_min,range_max,1,0) > trig_threshold, {
						var arr =
						[ // if the normalised value exceeds trig_threshold, add coordinates too coor_coll
							store_index.asFloat.linlin(0,columns-1,0,1), // x
							idx.asFloat.linlin(0,rows-1,1,0), // y
							k.asFloat.linlin(range_min,range_max,1,0) // normalised value
						];
						coor_coll = coor_coll.add(arr);
						~coor_coll = coor_coll; // make it global
					});
					// this value will go in coordinates
					k.asFloat.linlin(range_min,range_max,1,0);
				};




				// update grid GUI shades
				views_full[index].do{
					arg currentView, counter;
					{ currentView.background_(Color.gray(shades[counter])) }.defer;
				};

			};

			0.02.wait;

	}}).play(AppClock);


	~previous = nil; // introduce the variable






	Tdef(\colorCheck, {//observes grid status, plays slices at need
		loop{
			views_full.do{
				arg view, index;
				//var pair, value;

				~pair = ~coor_coll.collect{
					arg item, i;
					[item[0], item[1]]
				};

				~value = ~coor_coll.collect{
					arg item, i;
					item[2]; // this is teh value list of active nodes (same order as pair)

				};


			};

			if(~coor_coll.size != 0, { // if there are coordinates,
				// do something with the coordinates and value
				// (only for trigger based applications)

			});

			0.05.wait;
		}
	}).play;



*/



	~language = "en"; // default language English
	~lang = "en"; // default language English

	~parolaDurate = [];
	~indici_parola = [];
	~punct_cache = []; // cache per punteggiatura, così sono messi sempre alla fine della parola, prima dello spazio. è richiesta quando premi spazio, ma viene riempita dai segni di punteggiatura fino a là.
	~durataLinea = []; // resetta array composito di tutta la riga quando premi invio
	~sideQuest = 0;
	~sillabe_assegnabili = []; // resetta array di sillabe assegnabili
	~lista_indici = []; // resetta array sequenza di simboli

	~used_syl = []; // reset list of encountered syllables (each new one is stored once)
	~assignable_samples = (0..~sounds.size).asSet;
	~cached_pairs = Dictionary.new(~sounds.size); // reset dictionary
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

		{49} // BARRA SPAZIATRICE
		{
			if(~sideQuest==0 && ~currentLine.isNumber.not, {

				~parolaDurate = []; // reset array di durate (n elementi, uno per sillaba) -> durata parola
				~indici_parola = [];

				// se c'è solo punteggiatura nella cache, sequenziala prima di processare parole
				// se c'è solo punteggiatura e non parole quando premi spazio, quelle pause vengono aggiunte da sole
				~durataLinea = ~durataLinea++~punct_cache; //aggiungi eventuali rest (punteggiatura) nella cache
				~punct_cache = []; // svuota cache comunque prima di analizzare la parola;

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
						.reject(_ == $|).reject(_ == $>).reject(_ == $=)
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
					(~lastWord != "nil") && (~lastWord.isNumber.not),

					{
						// DA QUI SUBENTRA PYPHEN.

						// DIVIDI IN SILLABE.

						~currentWord_syllables = ~get_sylabs.(~language, ~lastWord)
						.reject(_ == $,).reject(_ == $.).reject(_ == $+).reject(_ == $!).reject(_ == $;).reject(_ == $:).reject(_ == $|)
						.reject(_ == $?).reject(_ == $/).reject(_ == $@).reject(_ == $\t ).reject(_.isNumber)/*.split($-)*/;// dividila in sillabe;


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

							~sillabaDurate = []; // reset - array di durate (un beat)
							~moltiplica_indice_copie = [];

							// per ogni sillaba (i) segna numero di lettere
							~lettere_inSillaba = ~currentWord_syllables.at(i).size; // n lettere in sillaba

							// depending on modeselector, decidi come rappresentare sillabe
							switchMode = ~modeSelector.value; // local modeSelector proxy

							//>>>>>>>>   		//switch
							passSwitch = switch (switchMode)

							{0} // Lettera-centrico
							{
								// per ogni lettera nella sillaba
								~lettere_inSillaba.do{ // per ogni lettera, metti in array di durate
									arg j;
									~sillabaDurate = ~sillabaDurate.insert(
										j, (1/(~lettere_inSillaba*2)).round(0.025)
									);
								};

								// RICAVA DURATE PER ARRAY DURATE


								if(	// se è ultima istanza, selezionala per durate
									~sillabaDurate.size === ~lettere_inSillaba,
									// trasferiscila in parolaDurate
									{
										~parolaDurate = ~parolaDurate.add(~sillabaDurate*~fattore_tempo);

										// CONTROLLA DA ELENCO SILLABE USATE PER ARRAY DI SAMPLES

										// prepara anche array di buffers
										// checka sillabe all'interno dell'array "unique", per cioccare l'indice
										if( // se hai una nuova sillaba..
											~cached_pairs.keys.asArray
											.includesEqual(~currentWord_syllables.at(i).asString)
											.not,
											{//.. salva una nuova associazione nel dizionario cached_pairs

												// sample picked random from the assignable set
												~bingo_sample_num = ~assignable_samples.asArray.flatten[~sounds.size.rand];

												// crea associazione sillaba corrente (nuova)
												~cached_pairs = ~cached_pairs.put(
													~currentWord_syllables.at(i).asString,
													~bingo_sample_num);
												// finally, remove the random picked sample number from the set of assignable samples;
												~assignable_samples.remove(~bingo_sample_num);
												"nuova sillaba aggiunta".postln;

												// operazioni per array buffers:
												~indice_sillaba = ~cached_pairs.at(~currentWord_syllables.at(i).asString);
												~moltiplica_indice_copie = Array.fill(
													~lettere_inSillaba, {~indice_sillaba}
												);
												~indici_parola = ~indici_parola.insert(i, ~moltiplica_indice_copie);
											},

											{ // se invece hai già incontrato quella sillaba, parsa il value corrispondente
												if(
													~cached_pairs.keys.asArray
													.includesEqual(~currentWord_syllables.at(i).asString),
													{
														// stessa roba diocan?
														~indice_sillaba = ~cached_pairs.at(~currentWord_syllables.at(i).asString);
														~moltiplica_indice_copie = Array.fill(
															~lettere_inSillaba, {~indice_sillaba});
														~indici_parola = ~indici_parola.insert(i, ~moltiplica_indice_copie);
													},
													{"uopssss....".postln}
												);
											}
										);

									},
								);
							}

							// RICAVA DURATE PER ARRAY DURATE

							{1} // Sillaba-centrico
							{
								// essenzialmente mette solo un valore di durata denetro sillabaDurate
								~sillabaDurate = ~sillabaDurate.add(~lettere_inSillaba*0.125);
								~parolaDurate = ~parolaDurate.add(~sillabaDurate*~fattore_tempo);
								//////////////////////////////////////////////////////////////////////

								// CONTROLLA DA ELENCO SILLABE USATE PER ARRAY DI SAMPLES

								// prepara anche array di buffers
								// checka sillabe all'interno dell'array "unique", per cioccare l'indice
								//~now = ~currentWord_syllables.at(i).asString;
								if( // se hai una nuova sillaba..
									~cached_pairs.keys.asArray
									.includesEqual(~currentWord_syllables.at(i).asString)
									.not,
									{//.. salva una nuova associazione nel dizionario cached_pairs

										// sample picked random from the assignable set
										~bingo_sample_num = ~assignable_samples.asArray.flatten[~sounds.size.rand];
										// crea associazione sillaba corrente (nuova)
										~cached_pairs = ~cached_pairs.put(
											~currentWord_syllables.at(i).asString,
											~bingo_sample_num);
										// finally, remove the random picked sample number from the set of assignable samples;
										~assignable_samples.remove(~bingo_sample_num);
										"nuova sillaba aggiunta".postln;

										// operazioni per array buffers:
										~indice_sillaba = ~cached_pairs.at(~currentWord_syllables.at(i).asString);
										~moltiplica_indice_copie = Array.fill(
											~lettere_inSillaba, {~indice_sillaba}
										);
										~indici_parola = ~indici_parola.insert(i, ~moltiplica_indice_copie);
									},

									{ // se invece hai già incontrato quella sillaba, parsa il value corrispondente
										if(
											~cached_pairs.keys.asArray
											.includesEqual(~currentWord_syllables.at(i).asString),
											{
												// stessa roba diocan?
												~indice_sillaba = ~cached_pairs.at(~currentWord_syllables.at(i).asString);
												~moltiplica_indice_copie = Array.fill(
													~lettere_inSillaba, {~indice_sillaba});
												~indici_parola = ~indici_parola.insert(i, ~moltiplica_indice_copie);
											},
											{"uopssss....".postln}
										);
									}
								);


							}
							{} {};
						};

						~sillabe_assegnabili = ~sillabe_assegnabili.add(~indici_parola);
						~lista_indici = ~sillabe_assegnabili.flatten;

						// infine, aggiungi parola appena emessa dentro la Linea
						~durataLinea = ~durataLinea.add(~parolaDurate.flatten).flatten;
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

		{36} // al premere INVIO
		{
			//var numeroCorrente;
			// aggiorna durations alla fine della sequenza di quando premi spazio
			// e aggiungi eventuali rest (punteggiatura) presente nella cache

			//~durataLinea = ~durataLinea++~punct_cache;

			// update params of interest (why not rate? funziona anche senza?)
			//~stretch = 60/~tempo*~measure;
			~release = ~rel.clip(0.2, 5);
			~attack = ~atk.clip(0.01, 4);
			~tempo = ~bpm;

			if(~lista_indici.isEmpty.not && ~numeroCorrente.isNil.not, {
				~sequence_type_1 = ~lista_indici;
				("SEQ "++~numeroCorrente.asString++" : ").post; ~sequence_type_1.postln;
			});



			// AGGIUNGI ULTERIORE SWITCH: SE QUANDO PREMI INVIO C'è UN NUMERO,
			// RINOMINA PDEF PER ACCEDERE A QUEL NUMERO

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

			~parolaDurate = []; // reset array di durate (n elementi, uno per sillaba) -> durata parola
			~indici_parola = [];
			~punct_cache = [];
		}

		// PUNTEGGIATURA: aggiunge rests alla cache ~punt_cache. viene concatenata alla linea
		{43} // al premere VIRGOLA / PUNTO E VIRGOLA
		{
			if(modifiers==131072,
				{ // se maiusc, punto e virgola (1/4)
					~punct_cache = ~punct_cache.add(Rest((1/4)*~fattore_tempo)).flatten;
				},
				{ // altrimenti è virgola (1/16)
					~punct_cache = ~punct_cache.add(Rest((1/16)*~fattore_tempo)).flatten;
				}
			);
		}

		{47} // al premere PUNTO / DUE PUNTI
		{
			if(modifiers==131072,
				{ // se maiusc allora è due punti (1/8)
					~punct_cache = ~punct_cache.add(Rest((1/8)*~fattore_tempo)).flatten;
				},
				{ // altrimenti è punto (1)
					~punct_cache = ~punct_cache.add(Rest(1*~fattore_tempo)).flatten;
				}
			);
		}




		{30} // al premere + PLUS) // RESET PHRASE
		//{42} // al premere + PLUS) // RESET PHRASE
		{
			~durataLinea = []; // resetta array composito di tutta la riga quando premi invio
			~sillabe_assegnabili = []; // resetta array di sillabe assegnabili
			~lista_indici = []; // resetta array sequenza di simboli
			~sequence_type_1 = [];
			~punct_cache = [];
			"RESET".postln;
		}

		{nil} {"--".postln}


		{27} // al premere PUNTO INTERROGATIVO ( maiusc + ' )
		//{30} // al premere PUNTO INTERROGATIVO ( maiusc + ' )
		{
			if(modifiers == 131072,
				{
					"punto interrogativo".postln;
					~punct_cache = ~punct_cache.add((
						[1/6, Rest(1/6), 1/6, 1/6, Rest(1/6), 1/6]
						.flatten)*~fattore_tempo).flatten;
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
					~punct_cache = ~punct_cache.add([1/12, 1/12, 1/12]*~fattore_tempo).flatten;

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

		{10} // al premere tasto | VERTICAL LINE (maiusc + \) // STOP SEQ
		//{24} // al premere tasto _ UNDERSCORE (maiusc + -) // STOP
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
					compile = ~command.interpret;

					// evaluate these side quests after closing expression (/)

					// qui mappa i mini-comandi della side quest con le variabili globali
					// che controllano parametri musicali (lista in aggiornamento)
					/////////////////////////////////////////////////////////
					// PARAMETER 1: MODE SELECTION
					// change between mode 0 and 1. updated after +
					~param1 = switch (~mode.value)
					{0} // Lettera-centrico
					{~valore=0;
						// per ogni lettera nella sillaba
						~lettere_inSillaba.do{
							arg j;
							~sillabaDurate = ~sillabaDurate.insert(
								j, (1/(~lettere_inSillaba*2)).round(0.001));
						};
						///////////////////////////////////////////////////////////////////////////////////////
						// prepara anche array di buffers
						// checka sillabe all'interno dell'array "unique", per cioccare l'indice
						if(	// se è ultima istanza, selezionala per durate
							~sillabaDurate.size === ~lettere_inSillaba,
							// trasferiscila in parolaDurate
							{
								~parolaDurate = ~parolaDurate.add(~sillabaDurate*~fattore_tempo);

								// CONTROLLA DA ELENCO SILLABE USATE PER ARRAY DI SAMPLES

								// prepara anche array di buffers
								// checka sillabe all'interno dell'array "unique", per cioccare l'indice
								if( // se hai una nuova sillaba..
									~cached_pairs.keys.asArray
									.includesEqual(~currentWord_syllables.at(i).asString)
									.not,
									{//.. salva una nuova associazione nel dizionario cached_pairs

										// sample picked random from the assignable set
										~bingo_sample_num = ~assignable_samples.asArray.flatten[~sounds.size.rand];

										// crea associazione sillaba corrente (nuova)
										~cached_pairs = ~cached_pairs.put(
											~currentWord_syllables.at(i).asString,
											~bingo_sample_num);
										// finally, remove the random picked sample number from the set of assignable samples;
										~assignable_samples.remove(~bingo_sample_num);
										"nuova sillaba aggiunta".postln;

										// operazioni per array buffers:
										~indice_sillaba = ~cached_pairs.at(~currentWord_syllables.at(i).asString);
										~moltiplica_indice_copie = Array.fill(
											~lettere_inSillaba, {~indice_sillaba}
										);
										~indici_parola = ~indici_parola.insert(i, ~moltiplica_indice_copie);
									},

									{ // se invece hai già incontrato quella sillaba, parsa il value corrispondente
										if(
											~cached_pairs.keys.asArray
											.includesEqual(~currentWord_syllables.at(i).asString),
											{
												// stessa roba diocan?
												~indice_sillaba = ~cached_pairs.at(~currentWord_syllables.at(i).asString);
												~moltiplica_indice_copie = Array.fill(
													~lettere_inSillaba, {~indice_sillaba});
												~indici_parola = ~indici_parola.insert(i, ~moltiplica_indice_copie);
											},
											{"uopssss....".postln}
										);
									}
								);

							},
						);

						if(	// se è ultima istanza, selezionala per durate
							~sillabaDurate.size === ~lettere_inSillaba,
							// trasferiscila in parolaDurate
							{
								~parolaDurate = ~parolaDurate.add(
									~sillabaDurate*~fattore_tempo)
							},
						);
					}

					{1} // Sillaba-centrico
					{~valore=1;
						// essenzialmente mette solo un valore di durata denetro sillabaDurate
						~sillabaDurate = ~sillabaDurate.add(~lettere_inSillaba*0.125);
						~parolaDurate = ~parolaDurate.add(~sillabaDurate*~fattore_tempo);
						//////////////////////////////////////////////////////////////////////

						// CONTROLLA DA ELENCO SILLABE USATE PER ARRAY DI SAMPLES

						// prepara anche array di buffers
						// checka sillabe all'interno dell'array "unique", per cioccare l'indice
						//~now = ~currentWord_syllables.at(i).asString;
						if( // se hai una nuova sillaba..
							~cached_pairs.keys.asArray
							.includesEqual(~currentWord_syllables.at(i).asString)
							.not,
							{//.. salva una nuova associazione nel dizionario cached_pairs

								// sample picked random from the assignable set
								~bingo_sample_num = ~assignable_samples.asArray.flatten[~sounds.size.rand];
								// crea associazione sillaba corrente (nuova)
								~cached_pairs = ~cached_pairs.put(
									~currentWord_syllables.at(i).asString,
									~bingo_sample_num);
								// finally, remove the random picked sample number from the set of assignable samples;
								~assignable_samples.remove(~bingo_sample_num);
								"nuova sillaba aggiunta".postln;

								// operazioni per array buffers:
								~indice_sillaba = ~cached_pairs.at(~currentWord_syllables.at(i).asString);
								~moltiplica_indice_copie = Array.fill(
									~lettere_inSillaba, {~indice_sillaba}
								);
								~indici_parola = ~indici_parola.insert(i, ~moltiplica_indice_copie);
							},

							{ // se invece hai già incontrato quella sillaba, parsa il value corrispondente
								if(
									~cached_pairs.keys.asArray
									.includesEqual(~currentWord_syllables.at(i).asString),
									{
										// stessa roba diocan?
										~indice_sillaba = ~cached_pairs.at(~currentWord_syllables.at(i).asString);
										~moltiplica_indice_copie = Array.fill(
											~lettere_inSillaba, {~indice_sillaba});
										~indici_parola = ~indici_parola.insert(i, ~moltiplica_indice_copie);
									},
									{"uopssss....".postln}
								);
							}
						);
					}


					{nil} {"nil param1".postln};


					///////////////////////////////////////////////////
					// PARAMETER 2: FATTORE TEMPO (0-2)
					~param2 = ~tFact;
					~fattore_tempo = ~param2.clip(0.1,4);

					"fattore tempo: "++~fattore_tempo.postln;
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

		{41} // al premere tasto @ (alt + ò) // OPEN EXPRESSION
		//{12} // al premere tasto @ (alt + ò) // OPEN EXPRESSION
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
)

////////////////////////////////////////////////////////////////////////////////////////////
