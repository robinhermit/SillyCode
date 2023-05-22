(
// SILLABATORE
/*
Server
Server.default.options.device = "BlackHole 2ch";
Server.default.options.device = "Built-in Output";
*/
////////////////////////////////////////////////////////////////////////////////////////////////////////
// I N I Z I A L I Z Z A //////////////////////////////////////
/////////////////////////////////////////////////7

~aa = {
// give it a few seconds to process this

var merged, unique;

merged = CSVFileReader.read(thisProcess.nowExecutingPath.dirname+/+"merged_files.csv", true, true);
~merged = merged.flatten;

">>>>> 25 % >>>>>>>>>>> Vocabulary: ready.".postln;

unique = CSVFileReader.read(thisProcess.nowExecutingPath.dirname+/+"tuttesillabe_norep_list.csv", true, true);
~unique = unique.flatten;
">>>>> 50 % >>>>>>>>>>>> Unique-syllables array: ready.".postln;

// these functions make it look like a paper dictionary (look letter 1, then letter 2 etc.)
~makeDict = { arg stringPairs;
	var dict = IdentityDictionary.new;
	stringPairs.pairsDo {
		arg str, hyphenation;
		~putOneString.(dict, str, hyphenation) };
	dict
};

~putOneString = { arg dict, str, hyphenation;
	var level = dict, sublevel;
	str.do { arg char;
		sublevel = level[char];
		if(sublevel.isNil) {
			sublevel = IdentityDictionary.new;
			level.put(char, sublevel);
		};
		level = sublevel;
	};
	level[\wholeString] = str;
	level[\hyphenation] = hyphenation;
};

~matchString = { |dict, str|
	var level = dict;
	var stream = CollStream(str);
	var char;
	while {
		char = stream.next;
		if(char.notNil) {
			level = level[char];
		};
		char.notNil and: { level.notNil }
	};
	// and here, retrieve hyphenation
	level.tryPerform(\at, \hyphenation)
};
">>>>>> 75 % >>>>>>>>>>> Vocabulary function.".postln;
};

//////////////////////

~bb = {
// this takes a few seconds
d = ~makeDict.(~merged); ">>>>>> 100 % >>>>>>>>>>>>> Association words-syllables formed".postln;
};


~cc = {
// ready to go! now you only have to insert a word here as string. if it doesn't exist, it'll print nil, otherwise it'll print the correct hyphenation
var testrun;
testrun = ~matchString.(d, "cangiante");
if(testrun.isNil.not, {"prova, prova, sa, sa, uno due".postln});
};

////////////////////////////////////////////////////////////////////////////////////////////////////



////////////////////////////////////////////////////////////////////////////////////////////////////////
// S Y N T H D E F S,  P A T T E R N S, B U F F E R S //////////////////////////////////////
/////////////////////////////////////////////////

~dd = {
s.options.numBuffers = 6000; // then reboot to save the changees
s.reboot;

s.waitForBoot{
//s.
Buffer.freeAll;
~sounds = Array.new;
~folder = PathName.new(thisProcess.nowExecutingPath.dirname+/+"Samples/");
// need to increase max buf number (1024)
//~folderEntries
~folder.entries.scramble.do({
	arg path;
	~sounds = ~sounds.add(Buffer.read(s, path.fullPath));
});
SynthDef(\click, {
	arg freq=100;
	var sig;
	sig = Ringz.ar(Impulse.ar(0.1), [freq, freq+1], 0.1) * Line.ar(1,0,0.1,1,0,2);
	sig = LPF.ar(sig, 150);
	Out.ar(0, sig);
}).add;

SynthDef(\playbuf_test_mono, {
	arg amp = 1, out = 0, buf,
	rate = 1, t_trig = 1, start = 0,
	atk = 0.01, rel = 0.5, da = 2;
	var sig, env,
	attack, release, bufdur, bufscale;
	bufdur = BufDur.kr(buf);
	bufscale = BufRateScale.ir(buf);
	attack = atk.clip(0.005, bufdur-(bufdur/10));
	release = rel.clip(0.1, 5);
	sig = PlayBuf.ar(1, buf, bufscale* rate, t_trig, start, doneAction: da);
	env = EnvGen.ar(Env.asr(attack, 1, release, -6), t_trig, doneAction: da);
	sig = sig * env;
	sig = sig * amp;
	Out.ar(out,sig!2);
}).add;

SynthDef(\playbuf_test_stereo, {
	arg amp = 1, out = 0, buf,
	rate = 1, t_trig = 1, start = 0,
	atk = 0.01, rel = 0.5, da = 2;
	var sig, env,
	attack, release, bufdur, bufscale;
	bufdur = BufDur.kr(buf);
	bufscale = BufRateScale.ir(buf);
	attack = atk.clip(0.005, bufdur-(bufdur/10));
	release = rel.clip(0.1, 5);
	sig = PlayBuf.ar(2, buf, bufscale * rate, t_trig, start, doneAction: da);
	env = EnvGen.ar(Env.asr(attack, 1, release, -6), t_trig, doneAction: da);
	sig = sig * env;
	sig = sig * amp;
	Out.ar(out,sig!2);
}).add;
}
};
//////////////////////////////////////////////////////////////////////////////////////////////7


Synth(\click); // audio test
Synth.new(\playbuf_test_stereo, [\buf, ~sounds[(0..1084).choose].bufnum, \rate, (1..12).choose.midiratio]);


/*
// CURRENT PDEFS
(

Pdef.clear; Pdef.removeAll;

// per inizializzare

Pdef(\click_pdef, Pbind(
	\instrument, \click,
	\dur, Pdefn(\dur, Pseq([2], inf)),
)).play;

Pdef(\playbuf_pdef, Pbind(
	\instrument, \playbuf_test_stereo,
	\dur, Pseq([2],inf),
	\stretch, ~stretch,
	\buf, Prand(~sounds[7..12], inf),
	\rate, 1
)).play(quant:~stretch);
)

// per cambiare parametri mantenendo quant

(
~rate = 1;

Pdef(\playbuf_1).play;
Pdef(\playbuf_2).play;
Pdef(\playbuf_3).play;
Pdef(\playbuf_4).play;
Pdef(\playbuf_5).play;
Pdef(\playbuf_6).play;
Pdef(\playbuf_7).play;
Pdef(\playbuf_8).play;
Pdef(\playbuf_9).play;
Pdef(\playbuf_0).play;

)
*/

/*
// SELEZIONA MODO

// 0 => Lettera-centrico: ogni sillaba dura 1 beat; ogni parola è una battuta di n beat = numero beats (sillabe), e ogni verso è una frase di lunghezza m = numero battute (parole). Ogni lettera nella sillaba è una frazione di un beat; tutte le sillabe di una parola sono di uguale durata, e dunque ciascuna lettera dura 1 (lunghezza beat) fratto numero di lettere. Esempio:
// "Esempio, questa stringa" would be (1+1+1), (1+1), (1+1) beats long = 7/4 signature. In the first word there are 3 sillables (1+1+1) and they have respectively 1,3 and 3 letters each. this means that the beats of this words would be divided as
// [1 ](E-), [0.3,0.3,0.3] (-sem-), [0.3 0.3 0.3] (-pio). The same operation is repeated for each word and the results chained together.
// Essentially, each letter is sonified as a note event, falling with a beat (representation of a standard sillable duration). This will produce faster subdivision for syllables with many letters, and a whole beat for syllables of 1 letter.

// 1 => Sillaba-centrico: ogni sillaba è una nota di durata variabile in un range. Ogni parola è una battuta di n beats (sillabe); ogni verso è una frase di m battute (parole). L'unica sostanziale differenza da 0 è che ciascuna sillaba è una nota di lunghezza proporzionale al numero di lettere; ciascuna lettera è sonificata come unità di 0.25 di durata relativa che può essere stacked together. Quindi di fatto una sillaba è un solo suono/evento/nota, anzichè un array di eventi/note come 0.
/*
DURATA SILLABA in relazione a numero di lettere (∂ = lettera generica)
=> ∂ = 0.25
=> ∂∂ = 0.5
=> ∂∂∂ = 0.75
=> ∂∂∂∂ = 1
=> ∂∂∂∂∂ = 1.25
*/

//~modeSelector = 0; //

//~bpm = 120;
//~measure = 4;
//~stretch = (60/~bpm)*~measure;


*/


////////////////////////////////////////////////////////////////////////////////////////////////////////
// G U I   F U N C T I O N S //////////////////////////////////////
/////////////////////////////////////////////////7

~ee = {// launch only once
// launch GUI
//
Pdef(\playbuf_1, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).play(quant:~stretch);
Pdef(\playbuf_2, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).play(quant:~stretch);
Pdef(\playbuf_3, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).play(quant:~stretch);
Pdef(\playbuf_4, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).play(quant:~stretch);
Pdef(\playbuf_5, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).play(quant:~stretch);
Pdef(\playbuf_6, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).play(quant:~stretch);
Pdef(\playbuf_7, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).play(quant:~stretch);
Pdef(\playbuf_8, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).play(quant:~stretch);
Pdef(\playbuf_9, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).play(quant:~stretch);
Pdef(\playbuf_0, Pbind(\instrument, \playbuf_test_stereo, \amp, 0)).play(quant:~stretch);

if( // SALVATAGGIO AUTOMATICO DEL TESTO SE C'è QUALCOSA DI SCRITTO
	t.string.notEmpty && t.string.notNil,
	{
		var file,date;
		date = Date.getDate.format("%Y%m%d_%H%M");
		file = File.new(thisProcess.nowExecutingPath.dirname+/+"Saved"+/+"SillyCode_"++date++".txt", "w");
		file.write(t.string.asString);
		file.close;
		"testo salvato".postln;
	},
	{}
);

// resetta parametri Side Quest
~fattore_tempo = 1; ~tFact=1;
~modeSelector = 0; ~mode=0;

~tempo = 120; ~bpm=120;
~measure = 4; ~division=4;
~stretch = (60/~tempo)*~measure;

~attack = 0.01; ~atk=0.01;
~release = 0.5; ~rel=0.5;
~rate = 1;

/*
Pdef(\playbuf_pdef, Pbind(
	//\instrument, Pseq([Pfunc{|ev|if (ev.buf.numChannels == 1) {\playbuf_test_mono} {\playbuf_test_stereo}}], inf),
	\instrument, \playbuf_test_stereo,
	\stretch, ~stretch,
	\dur, Pdefn(\dur, Pseq([1], inf)),
	\buf, Pdefn(\bufChoice, Pseq([~sounds[12].bufnum], inf)),
	\rel, Pdefn(\release, Pseq([~release], inf)),
	\atk, Pdefn(\attack, Pseq([~attack], inf)),
	\rate, Pdefn(\rate, Pseq([~rate], inf)),
)).play(quant: ~stretch);
*/

Window.closeAll;
//Tdef(\checkLoop).stop;
w = Window.new("SillyCode", Rect(0,0,width: (Window.screenBounds.width/2)+20,height: Window.screenBounds.height)).front;
z = PdefAllGui(20);


//~numeroCorrente = 1; // resetta pattern to defaule (1)

~parolaDurate = [];
~indici_parola = [];
~punct_cache = []; // cache per punteggiatura, così sono messi sempre alla fine della parola, prima dello spazio. è richiesta quando premi spazio, ma viene riempita dai segni di punteggiatura fino a là.
~durataLinea = []; // resetta array composito di tutta la riga quando premi invio
~sideQuest = 0;
~sillabe_assegnabili = []; // resetta array di sillabe assegnabili
~lista_indici = []; // resetta array sequenza di simboli
"RESET".postln;

t = TextView(w.asView,Rect(10,10, Window.screenBounds.width/2,Window.screenBounds.height-70))
    .focus(true)
	.palette_(QPalette.dark)
	.font_(Font("Courier",25), 5, 10)
	.hasVerticalScroller_(true);
// characters allowed in a binary operator: "!@%&*-+=|<>?/".
// keyboard functions

t.keyDownAction =
{ arg view, char, modifiers, unicode, keycode;

	~switch = switch (keycode) // KEY PARSER

	{49} // BARRA SPAZIATRICE
	{
		~parolaDurate = []; // reset array di durate (n elementi, uno per sillaba) -> durata parola
		~indici_parola = [];

		// se c'è solo punteggiatura nella cache, sequenziala prima di processare parole
		// se c'è solo punteggiatura e non parole quando premi spazio, quelle pause vengono aggiunte da sole
		~durataLinea = ~durataLinea++~punct_cache; //aggiungi eventuali rest (punteggiatura) nella cache
		~punct_cache = []; // svuota cache comunque prima di analizzare la parola;

		~currentLine = [t.currentLine.toLower
			.reject(_ == $,).reject(_ == $.).reject(_ == $+).reject(_ == $!).reject(_ == $;).reject(_ == $:)
			.reject(_ == $?).reject(_ == $/).reject(_ == $@).reject(_ == $().reject(_ == $))
			.split($ )].flatten; // dividi linea in parole

		// updata numeroCorrente ogni volta che premi spazio
		t.currentLine.size.do{
			arg i;
			if(
				t.currentLine[i].asString.interpret.isNumber,
				{~numeroCorrente=t.currentLine[0]},
				{}
			);
		};

		~currentLine_wordsnum = ~currentLine.flatten.size; // numero parole nella riga
		~lastWord = ~currentLine[~currentLine.size-1].asString; // prendi l'ultima parola

		if( // se l'ultima parola non è nil, trasformala in sequenza, altrimenti ignorala
			~lastWord.notNil,
			{
				~currentWord_syllables = ~matchString.(d, ~lastWord)
				.reject(_ == $,).reject(_ == $.).reject(_ == $+).reject(_ == $!).reject(_ == $;).reject(_ == $:)
				.reject(_ == $?).reject(_ == $/).reject(_ == $@).split($-);// dividila in sillabe

				~currentWord_syllables.postln; // postala sillabazione della parola

///////////////////////////////////////////////////////////////////////////////////////
					// per ogni sillaba, fai quanto segue
				~currentWord_syllables.size.do{
					arg i;
					var switchMode, passSwitch;

					~sillabaDurate = []; // reset - array di durate (un beat)
					~moltiplica_indice_copie = [];

					// per ogni sillaba (i) segna numero di lettere
					~lettere_inSillaba = ~currentWord_syllables.at(i).size; // n lettere in sillaba

					// depending on modeselector, decidi come rappresentare sillabe
					switchMode = ~modeSelector.value; // local modeSelector proxy

					//switch
					passSwitch = switch (switchMode)

					{0} // Lettera-centrico
					{ // per ogni lettera nella sillaba
						~lettere_inSillaba.do{ // per ogni lettera, metti in array di durate
							arg j;
							~sillabaDurate = ~sillabaDurate.insert(j, (1/(~lettere_inSillaba*4)).round(0.0001));
						};



						if(	// se è ultima istanza, selezionala per durate
							~sillabaDurate.size === ~lettere_inSillaba,
							// trasferiscila in parolaDurate
							{
								~parolaDurate = ~parolaDurate.add(~sillabaDurate*~fattore_tempo);

								// prepara anche array di buffers
								// checka sillabe all'interno dell'array "unique", per cioccare l'indice
								~now = ~currentWord_syllables.at(i).asString;
								if(
									~unique.includesEqual(~now),
									{// costruisci array di bufnums
										"riconosciuta".postln;
										~indice_sillaba = ~unique.detectIndex({ arg k; k == ~now});

										~moltiplica_indice_copie = Array.fill(
											~lettere_inSillaba, {~indice_sillaba}
										);
										~indici_parola = ~indici_parola.insert(i, ~moltiplica_indice_copie);

										//~indici_parola.postln; // ogni sillaba - CORRETTA

										//~lista_indici.postln;
										//~lista_indici=~lista_indici.flatten;
									},
									{"sconosciuta".postln}
								);
							},
						);
					}

					{1} // Sillaba-centrico
					{ // essenzialmente mette solo un valore di durata denetro sillabaDurate
						~sillabaDurate = ~sillabaDurate.add(~lettere_inSillaba*0.125);
						~parolaDurate = ~parolaDurate.add(~sillabaDurate*~fattore_tempo);
					//////////////////////////////////////////////////////////////////////
					// prepara anche array di buffers
					// checka sillabe all'interno dell'array "unique", per cioccare l'indice
						~now = ~currentWord_syllables.at(i).asString;
						if( // se la sillaba corrente esiste,
							~unique.includesEqual(~now),
							{
								"riconosciuta".postln;
								~indice_sillaba = ~unique.detectIndex({ arg item, i; item == ~now});

								~lista_SILLABE_USATE = ~sillabe_assegnabili.asSet.asArray.sort;// < < < ignora

								// qui per creare sequenza letterale (con ripetizioni), per esempio per bufnum
								~lista_indici = ~sillabe_assegnabili.flatten;
							},
							{"silaba sconosciuta".postln;}
						);

					}
					{} {};
				};

				~sillabe_assegnabili = ~sillabe_assegnabili.add(~indici_parola);
				~lista_indici = ~sillabe_assegnabili.flatten;

				// infine, aggiungi parola appena emessa dentro la Linea
				~durataLinea = ~durataLinea.add(~parolaDurate.flatten).flatten;
				// sequenza simbolica (sillabe -> simboli a scelta)
				~sequence_type_1 = ~lista_indici.linlin(0, ~unique.size, 0, ~folder.entries.size).round.asInteger;
				// (determina quali sample assegnare, da cartella progetto
			},

			// se non Nil, ma
			{
				"__".postln;
			} //se è Nil -->qualcosa ???
		);
		~durataLinea.postln;
		~durataLinea = ~durataLinea++~punct_cache; //aggiungi eventuali rest (punteggiatura) nella cache

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

		~sequence_type_1 = ~lista_indici.linlin(
			0, ~unique.size, 0, ~folder.entries.size).round.asInteger;


		// AGGIUNGI ULTERIORE SWITCH: SE QUANDO PREMI INVIO C'è UN NUMERO,
		// RINOMINA PDEF PER ACCEDERE A QUEL NUMERO

		// al premere invio, itera attraverso la riga corrente.
		// se trova numeri, salva l'ultimo in numeroCorrente
		// e attiva lo switch corrispondente
/*
		t.currentLine.size.do{
			arg i;
			if(t.currentLine[i].asString.interpret.isNumber,
				{
					numeroCorrente = t.currentLine[i];
					numeroCorrente.postln; // latest number in line
				},
				{
					//"nessun numero".postln;
				}
			);
		};
*/
		//~numeroCorrente=numeroCorrente;
		~pdefs_choice = switch
		(~numeroCorrente.value.asString.asInteger)

		{1}
		{
			var stretch, durataLinea, sequence_type_1, attack, release, rate;
			stretch=~stretch; durataLinea=~durataLinea; sequence_type_1=~sequence_type_1; attack=~attack; release=~release; rate=~rate;

			Pdef(\playbuf_1, Pbind(
				\instrument, \playbuf_test_stereo,
				\stretch, stretch,
				\dur, Pseq(durataLinea, inf),
				\buf, Pseq(sequence_type_1, inf).trace,
				\rel, Pseq([release], inf),
				\atk, Pseq([attack], inf),
				\rate, Pseq([rate], inf),
			)).play(quant: stretch);

		}
		{2}
		{
			var stretch, durataLinea, sequence_type_1, attack, release, rate;
			stretch=~stretch; durataLinea=~durataLinea; sequence_type_1=~sequence_type_1; attack=~attack; release=~release; rate=~rate;

			Pdef(\playbuf_2, Pbind(
				\instrument, \playbuf_test_stereo,
				\stretch, stretch,
				\dur, Pseq(durataLinea, inf),
				\buf, Pseq(sequence_type_1, inf).trace,
				\rel, Pseq([release], inf),
				\atk, Pseq([attack], inf),
				\rate, Pseq([rate], inf),
			)).play(quant: stretch);
		}
		{3}
		{
			var stretch, durataLinea, sequence_type_1, attack, release, rate;
			stretch=~stretch; durataLinea=~durataLinea; sequence_type_1=~sequence_type_1; attack=~attack; release=~release; rate=~rate;

			Pdef(\playbuf_3, Pbind(
				\instrument, \playbuf_test_stereo,
				\stretch, stretch,
				\dur, Pseq(durataLinea, inf),
				\buf, Pseq(sequence_type_1, inf),
				\rel, Pseq([release], inf),
				\atk, Pseq([attack], inf),
				\rate, Pseq([rate], inf),
			)).play(quant: stretch);
		}
		{4}
		{
			var stretch, durataLinea, sequence_type_1, attack, release, rate;
			stretch=~stretch; durataLinea=~durataLinea; sequence_type_1=~sequence_type_1; attack=~attack; release=~release; rate=~rate;

			Pdef(\playbuf_4, Pbind(
				\instrument, \playbuf_test_stereo,
				\stretch, stretch,
				\dur, Pseq(durataLinea, inf),
				\buf, Pseq(sequence_type_1, inf),
				\rel, Pseq([release], inf),
				\atk, Pseq([attack], inf),
				\rate, Pseq([rate], inf),
			)).play(quant: stretch);
		}
		{5}
		{
			var stretch, durataLinea, sequence_type_1, attack, release, rate;
			stretch=~stretch; durataLinea=~durataLinea; sequence_type_1=~sequence_type_1; attack=~attack; release=~release; rate=~rate;

			Pdef(\playbuf_5, Pbind(
				\instrument, \playbuf_test_stereo,
				\stretch, stretch,
				\dur, Pseq(durataLinea, inf),
				\buf, Pseq(sequence_type_1, inf),
				\rel, Pseq([release], inf),
				\atk, Pseq([attack], inf),
				\rate, Pseq([rate], inf),
			)).play(quant: stretch);					}
		{6}
		{
			var stretch, durataLinea, sequence_type_1, attack, release, rate;
			stretch=~stretch; durataLinea=~durataLinea; sequence_type_1=~sequence_type_1; attack=~attack; release=~release; rate=~rate;

			Pdef(\playbuf_6, Pbind(
				\instrument, \playbuf_test_stereo,
				\stretch, stretch,
				\dur, Pseq(durataLinea, inf),
				\buf, Pseq(sequence_type_1, inf),
				\rel, Pseq([release], inf),
				\atk, Pseq([attack], inf),
				\rate, Pseq([rate], inf),
			)).play(quant: stretch);
		}
		{7}
		{
			var stretch, durataLinea, sequence_type_1, attack, release, rate;
			stretch=~stretch; durataLinea=~durataLinea; sequence_type_1=~sequence_type_1; attack=~attack; release=~release; rate=~rate;

			Pdef(\playbuf_7, Pbind(
				\instrument, \playbuf_test_stereo,
				\stretch, stretch,
				\dur, Pseq(durataLinea, inf),
				\buf, Pseq(sequence_type_1, inf),
				\rel, Pseq([release], inf),
				\atk, Pseq([attack], inf),
				\rate, Pseq([rate], inf),
			)).play(quant: stretch);
		}
		{8}
		{
			var stretch, durataLinea, sequence_type_1, attack, release, rate;
			stretch=~stretch; durataLinea=~durataLinea; sequence_type_1=~sequence_type_1; attack=~attack; release=~release; rate=~rate;

			Pdef(\playbuf_8, Pbind(
				\instrument, \playbuf_test_stereo,
				\stretch, stretch,
				\dur, Pseq(durataLinea, inf),
				\buf, Pseq(sequence_type_1, inf),
				\rel, Pseq([release], inf),
				\atk, Pseq([attack], inf),
				\rate, Pseq([rate], inf),
			)).play(quant: stretch);
		}
		{9}
		{
			var stretch, durataLinea, sequence_type_1, attack, release, rate;
			stretch=~stretch; durataLinea=~durataLinea; sequence_type_1=~sequence_type_1; attack=~attack; release=~release; rate=~rate;

			Pdef(\playbuf_9, Pbind(
				\instrument, \playbuf_test_stereo,
				\stretch, stretch,
				\dur, Pseq(durataLinea, inf),
				\buf, Pseq(sequence_type_1, inf),
				\rel, Pseq([release], inf),
				\atk, Pseq([attack], inf),
				\rate, Pseq([rate], inf),
			)).play(quant: stretch);
		}
		{0}
		{
			var stretch, durataLinea, sequence_type_1, attack, release, rate;
			stretch=~stretch; durataLinea=~durataLinea; sequence_type_1=~sequence_type_1; attack=~attack; release=~release; rate=~rate;

			Pdef(\playbuf_0, Pbind(
				\instrument, \playbuf_test_stereo,
				\stretch, stretch,
				\dur, Pseq(durataLinea, inf),
				\buf, Pseq(sequence_type_1, inf),
				\rel, Pseq([release], inf),
				\atk, Pseq([attack], inf),
				\rate, Pseq([rate], inf),
			)).play(quant: stretch);
		};

/*
		Pdef(\playbuf_pdef, Pbind(
			//\instrument, Pseq([Pfunc{|ev|if (ev.buf.numChannels == 1) {\playbuf_test_mono} {\playbuf_test_stereo}}], inf),
			\instrument, \playbuf_test_stereo,
			\stretch, ~stretch,
			\dur, Pdefn(\dur, Pseq(~durataLinea, inf)).trace,
			\buf, Pdefn(\bufChoice, Pseq(~sequence_type_1, inf).trace),
			\rel, Pdefn(\release, Pseq([~release], inf)),
			\atk, Pdefn(\attack, Pseq([~attack], inf)),
			\rate, Pdefn(\rate, Pseq([~rate], inf)),
		)).quant_(~stretch);
*/
		"> > > > EXECUTE "++~numeroCorrente.asString.postln;
		~punct_cache = []; // svuota cache punteggiatura


		~tempo=~bpm;
		~measure=~division;
		~stretch = 60/~tempo*~measure;

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
	{
		~durataLinea = []; // resetta array composito di tutta la riga quando premi invio
		~sillabe_assegnabili = []; // resetta array di sillabe assegnabili
		~lista_indici = []; // resetta array sequenza di simboli
		~sequence_type_1 = [];
		"RESET".postln;
	}

	{nil} {"--".postln}


	{27} // al premere PUNTO INTERROGATIVO ( maiusc + ' )
	{
		if(modifiers == 131072,
			{
				"punto interrogativo".postln;
				~punct_cache = ~punct_cache.add((
					[1/6, Rest(1/6), 1/6, 1/6, Rest(1/6), 1/6]
					.flatten)*~fattore_tempo).flatten;
			},

			{ // al premere solo APOSTROFO ( ' ) // devi riuscire a includere l', degl', dell', un', d', gl
				"apostrofo".postln

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

				q.play; // sampler
				//Pdef(\playbuf_pdef).play;
				~pdefs_play = switch
				(~numeroCorrente.value.asString.asInteger)
				{1} {Pdef(\playbuf_1).play(quant:~stretch); "UNO".postln;}
				{2} {Pdef(\playbuf_2).play(quant:~stretch); "DUE".postln;}
				{3} {Pdef(\playbuf_3).play(quant:~stretch); "TRE".postln;}
				{4} {Pdef(\playbuf_4).play(quant:~stretch); "QUATTRO".postln;}
				{5} {Pdef(\playbuf_5).play(quant:~stretch); "CINQUE".postln;}
				{6} {Pdef(\playbuf_6).play(quant:~stretch); "SEI".postln;}
				{7} {Pdef(\playbuf_7).play(quant:~stretch); "SETTE".postln;}
				{8} {Pdef(\playbuf_8).play(quant:~stretch); "OTTO".postln;}
				{9} {Pdef(\playbuf_9).play(quant:~stretch); "NOVE".postln;}
				{0} {Pdef(\playbuf_0).play(quant:~stretch); "DIECI".postln;};


				"PLAY|".postln;
			},
			{}

		);
	}

	{10} // al premere tasto | VERTICAL LINE (maiusc + \) // STOP SEQ
	{
	if(modifiers == 131072,
		{
			//p.stop; // click sound
			//q.stop; // sampler
			//Pdef(\playbuf_pdef).stop;

			// se premi STOP con un NUMERO prima, stoppa
			// Pdef(\playbuf_++NUMERO.asString)
			// altrimenti, by default stoppa NUMERO UNO


			t.currentLine.size.do{
				arg i;
				if( t.currentLine[i].asString.interpret.isNumber,
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
						~numeroCorrente=nil;
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

/*

			t.currentLine.size.do{
				arg i;
				if(
					t.currentLine[i].asString.interpret.isNumber,
					{
						~numeroCorrente = t.currentLine[i];
						~numeroCorrente.postln;
						~pdefs_choice = switch
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
					},
					{
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
*/
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
				~punct_cache = 0; //svuota cache punteggiatura per evitare di rappresentare roba tipo 0.2 etc. come rests
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
					{ // per ogni lettera nella sillaba
						~lettere_inSillaba.do{
							arg j;
							~sillabaDurate = ~sillabaDurate.insert(
							j, (1/(~lettere_inSillaba*4)).round(0.0001));
						};
///////////////////////////////////////////////////////////////////////////////////////
					// prepara anche array di buffers
					// checka sillabe all'interno dell'array "unique", per cioccare l'indice
					~now = ~currentWord_syllables.at(i).asString;
					if(
						~unique.includesEqual(~now),
						{// costruisci array di bufnums
							"riconosciuta".postln;
							~indice_sillaba = ~unique.detectIndex({ arg item, i;
								item == ~now});
							~moltiplica_indice_copie = Array.fill(
								~lettere_inSillaba, {~indice_sillaba});
							~indici_parola = ~indici_parola.add(~moltiplica_indice_copie);

							~sillabe_assegnabili = ~sillabe_assegnabili.add(
								~indici_parola);
							~lista_indici = ~sillabe_assegnabili.flatten;
							~lista_indici.postln;

						},
						{"sconosciuta".postln}
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
				{ // essenzialmente mette solo un valore di durata denetro sillabaDurate
					~sillabaDurate = ~sillabaDurate.add(~lettere_inSillaba*0.125);
					~parolaDurate = ~parolaDurate.add(~sillabaDurate*~fattore_tempo);
///////////////////////////////////////////////////////////////////////////////////////
					// prepara anche array di buffers
					// checka sillabe all'interno dell'array "unique", per cioccare l'indice
					~now = ~currentWord_syllables.at(i).asString;
					if( // se la sillaba corrente esiste,
						~unique.includesEqual(~now),
						{
							"riconosciuta".postln;
							~indice_sillaba = ~unique.detectIndex({ arg item, i;
								item == ~now});
							// qui per creare sequenza letterale (con ripetizioni), per esempio per bufnum
							~lista_indici = ~sillabe_assegnabili.flatten;
						},
						{"silaba sconosciuta".postln;}
					);

				}


				{nil} {"nil param1".postln};


///////////////////////////////////////////////////
				// PARAMETER 2: FATTORE TEMPO (0-2)
				~param2 = ~tFact;
				~fattore_tempo = ~param2.clip(0.1,4);

				//Pdefn(\dur, Pseq(~durataLinea*~fattore_tempo, inf));
				/*
				Pdef(\playbuf_pdef, Pbind(
					\dur, Pdefn(\dur, Pseq(~durataLinea*~fattore_tempo, inf)),
				)).quant_(~stretch);
				*/
				"fattore tempo: "++~fattore_tempo.postln;
////////////////////////////////////////////////////
				// PARAMETER 3: RELEASE BUF PLAYER (0.2 - 5)
				~param3 = ~rel;
				~release = ~rel.clip(0.2, 5);
				//Pdefn(\release, Pseq([~release], inf));
				/*
				Pdef(\playbuf_pdef, Pbind(
					\rel, Pdefn(\release, Pseq([~release], inf)),
				)).quant_(~stretch);
				*/
				"release: "++~release.asString.postln;
////////////////////////////////////////////////////
				// PARAMETER 4: ATK BUF PLAYER (0.01 - 4)
				~param4 = ~atk;
				~attack = ~atk.clip(0.01, 4);
				//Pdefn(\attack, Pseq([~attack], inf));
				/*
				Pdef(\playbuf_pdef, Pbind(
					\atk, Pdefn(\attack, Pseq([~attack], inf)),
				)).quant_(~stretch);
				*/
				"attack: "++~attack.asString.postln;

////////////////////////////////////////////////////
				// PARAMETER 5: RATE [-12, 12; default 1, reverse -1]

				~param5 = ~rate;
				/*
				Pdef(\playbuf_pdef, Pbind(
					\rate, Pdefn(\rate, Pseq([~rate], inf)),
				)).quant_(~stretch);
				*/
				"rate: "++~rate.asString.postln;

////////////////////////////////////////////////////
				// PARAMETER 6: TEMPO (10..500); default 120
				~param6 = ~bpm;
				~tempo = ~param6;

////////////////////////////////////////////////////
				// PARAMETER 7: MEASURE (1..16); default 4

				~param7 = ~division;
				~measure = ~param7;
				/////////////////////////////////////
				~stretch = (60/~tempo)*~measure;

////////////////////////////////////////////////////
				// PARAMETER 8: SCRAMBLE [0, 1]; default 0

/*
				~param8 = switch (~scramble.value)
				{0}
				{"no scramble".postln}
				{1}
				{
					1.do{
					Buffer.freeAll;
					~sounds = Array.new;
					~folder = PathName.new("/Users/Robin/Desktop/TUTTO/zzz_TextSonification/sampli/");
					~folder.entries.scramble.do({
						arg path;
						~sounds = ~sounds.add(Buffer.read(s, path.fullPath));
					});
					~scramble = 0; // turn off scramble after remixing folder entries
					};
				};
*/


				// pile all together

				Pdef(\playbuf_pdef, Pbind(
					//\instrument, Pfunc{|ev|if (ev.buf.numChannels == 1) {\playbuf_test_mono} {\playbuf_test_stereo}},
					\instrument, \playbuf_test_stereo,
					\stretch, ~stretch,
					\buf, Pdefn(\bufChoice, Pseq(~sequence_type_1, inf)),
					\dur, Pdefn(\dur, Pseq(~durataLinea*~fattore_tempo, inf)),
					\rel, Pdefn(\release, Pseq([~release], inf)),
					\atk, Pdefn(\attack, Pseq([~attack], inf)),
					\rate, Pdefn(\rate, Pseq([~rate], inf)),
				)).quant_(~stretch);
			},
			{}
		);
	}

	{41} // al premere tasto @ (alt + ò) // OPEN EXPRESSION
	{
		if(modifiers == 524288,
			{
				~sideQuest = 1;
				"open expression".postln;
			},
			{"no".postln}

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
/*
( // KEY TEST
w = Window.new("I catch keystrokes");
w.view.keyDownAction = { arg view, char, modifiers, unicode, keycode;  [view, char, modifiers, unicode, keycode].postln; };
w.front;
)
*/
/////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////
