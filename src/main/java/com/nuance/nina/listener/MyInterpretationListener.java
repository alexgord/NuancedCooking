/*
 * 
 * File: MyInterpretationListener.java
 * 
 * Copyright (C) 2015, Nuance Communications Inc. All Rights Reserved.
 * 
 */

package com.nuance.nina.listener;

import java.text.DateFormat;
import java.util.Date;

import com.nuance.nina.sample.R;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.nuance.nina.mmf.MMFController;
import com.nuance.nina.mmf.PromptType;
import com.nuance.nina.mmf.MMFFindMeaningResult.Concept;
import com.nuance.nina.mmf.listeners.Interpretation;
import com.nuance.nina.mmf.listeners.InterpretationError;
import com.nuance.nina.mmf.listeners.InterpretationError.Reason;
import com.nuance.nina.mmf.listeners.InterpretationListener;
import com.nuance.nina.ninasample.MainActivity;
import com.nuance.nina.ninasample.ViewAdapter.Views;

/**
 * Implementation of InterpretationListener
 * Handles the different states of the interpretation received from Nina
 *
 */
public class MyInterpretationListener implements InterpretationListener{
	private Activity myActivity;
	public  MyInterpretationListener(Activity activity){
		myActivity = activity;
	}

	/**
	 * Triggered when the interpretation is success
	 */
	@Override
	public void onInterpretation(final Interpretation data) {
		Handler handler = new Handler(Looper.getMainLooper()); 
		handler.post(new Runnable() {
			@Override
			public void run() {
				String prompt = "";
				switch(data.resultStatus){
				case NO_INPUT_TIMEOUT:
					prompt =  "I didn't hear anything. Can you please repeat your question?";

					MMFController.getInstance().playPrompt(prompt, PromptType.TEXT);
					displayMessage(((MainActivity)myActivity).getCurrentFragment(), prompt );
					((MainActivity)myActivity).log(DateFormat.getDateTimeInstance().format(new Date()) + ": Interpretation null");
					break;
				case NO_MATCH:
					prompt =  "Sorry, I wasn't able to recognize what you said";

					MMFController.getInstance().playPrompt(prompt, PromptType.TEXT);
					displayMessage(((MainActivity)myActivity).getCurrentFragment(), prompt );
					break;
				case SUCCESS:
					//Interpretation result of input text or recognized speech using self-served grammar or custom-grammar.
					//Contains: 
					// - Filtered input text: filteredInputText
					// - Raw input text from the user to find meaning of or recognized text by recognition: inputText
					// - Get the meaning with the highest confidence by Nina or null if there were no meanings extracted: getBestMeaning()
					//and more methods
					if (data.mmfFindMeaningResult != null){
						((MainActivity)myActivity).log(DateFormat.getDateTimeInstance().format(new Date()) + ": Interpretation");
						playResult(data);
					}
					//Container object for the recognition portion of a server result, consisting of one or more/
					//Contains:
					// - The text of the transcription: literal
					// - The confidence level, a higher number is more confident: confidence
					else if(data.mmfRecognitionResult != null) {
						((MainActivity)myActivity).log(DateFormat.getDateTimeInstance().format(new Date()) + ": Dictation recognized");
						playResult(data);
					}
					else{
						((MainActivity)myActivity).log(DateFormat.getDateTimeInstance().format(new Date()) + ": Interpretation unknown");
						playResult(data);
					}
					break;
				case SPEECH_NOT_DETECTED:
					prompt =  "Sorry, speech not detected";

					MMFController.getInstance().playPrompt(prompt, PromptType.TEXT);
					displayMessage(((MainActivity)myActivity).getCurrentFragment(), prompt );
					break;

				case RETRY:
					prompt =  "Sorry, please try again";

					MMFController.getInstance().playPrompt(prompt, PromptType.TEXT);
					displayMessage(((MainActivity)myActivity).getCurrentFragment(), prompt );
					break;

				case NO_MEANING:
					prompt =  "Sorry, no meaning";

					MMFController.getInstance().playPrompt(prompt, PromptType.TEXT);
					displayMessage(((MainActivity)myActivity).getCurrentFragment(), prompt );
					break;
				case SPEECH_TOO_EARLY:
					prompt =  "Sorry, speech too early";

					MMFController.getInstance().playPrompt(prompt, PromptType.TEXT);
					displayMessage(((MainActivity)myActivity).getCurrentFragment(), prompt );
					break;
				case TOO_MUCH_SPEECH_TIMEOUT:
					prompt =  "Sorry, too much speech";

					MMFController.getInstance().playPrompt(prompt, PromptType.TEXT);
					displayMessage(((MainActivity)myActivity).getCurrentFragment(), prompt );
					break;
				default:
					prompt =  "Sorry, no match";

					MMFController.getInstance().playPrompt(prompt, PromptType.TEXT);
					displayMessage(((MainActivity)myActivity).getCurrentFragment(), prompt );
					break;
				}
			}
		});
	}

	/**
	 * Triggered when there is an error in the interpretation
	 */
	@Override
	public void onInterpretationError(final InterpretationError data) {
		Handler handler = new Handler(Looper.getMainLooper()); 
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (data.reason != Reason.CANCELLED){
					((MainActivity)myActivity).log(DateFormat.getDateTimeInstance().format(new Date()) + ": Interpretation Error");
					displayAndPlayErrorMessage(((MainActivity)myActivity).getCurrentFragment(), data);
				}else{
					((MainActivity)myActivity).log(DateFormat.getDateTimeInstance().format(new Date()) + ": Interpretation Cancelled");
					MyEnergyLevelListener.updateLevelSoundBar(0);
				}
			}
		} );
	}

	/**
	 * Decomposes the data received from the interpretation
	 * @param data
	 */
	private void playResult(Interpretation data)
	{
		if (data.mmfFindMeaningResult != null && data.mmfFindMeaningResult.getBestMeaning() != null)
		{
			Concept[] concepts = data.mmfFindMeaningResult.getBestMeaning().concepts;
			String prompt = "";

			prompt = "I recognized " + data.mmfFindMeaningResult.getInputText() + " and found that ";

			for (Concept concept : concepts)
			{
				prompt = prompt + " " + concept.name + " is " + concept.value;
			}
			prompt = prompt.replaceAll("\\s+", " ").trim();
			MMFController.getInstance().playPrompt(prompt, PromptType.TEXT);
			prompt = prompt + ". Confidence: " + data.mmfFindMeaningResult.getBestMeaning().confidence;
			displayMessage(((MainActivity)myActivity).getCurrentFragment(), prompt);
		}
		else if(data.mmfRecognitionResult != null){
			String prompt = "";

			prompt = "I recognized " + data.mmfRecognitionResult.getBestTranscription();
			Views v = ((MainActivity) myActivity).getCurrentFragment();
			switch(v){
			case SAY:
				prompt = prompt + " but I wasn't able to understand.";
				break;
			default:
				break;
			}
			prompt = prompt.replaceAll("\\s+", " ").trim();
			MMFController.getInstance().playPrompt(prompt, PromptType.TEXT);
			displayMessage(((MainActivity)myActivity).getCurrentFragment(), "I recognized  " + data.mmfRecognitionResult.getBestTranscription() );
		}
		else{
			String prompt = "Unknown text";

			MMFController.getInstance().playPrompt(prompt, PromptType.TEXT);
			displayMessage(((MainActivity)myActivity).getCurrentFragment(), prompt );
		}
	}

	private void displayMessage(Views currentFragment, String message){
		switch(currentFragment){
		case SAY:
			final TextView resultSayLabel = (TextView) myActivity.findViewById(R.id.resultSayLabel);
			resultSayLabel.setText(message);

			final Button listenButton = (Button) myActivity.findViewById(R.id.listenButton);
			listenButton.setText("Listen");
			listenButton.setEnabled(false);
			break;
		case DICTATE:
			final TextView resultDictationLabel = (TextView) myActivity.findViewById(R.id.resultDictationLabel);
			resultDictationLabel.setText(message);

			final Button dictationButton = (Button) myActivity.findViewById(R.id.dictationButton);
			dictationButton.setText("Dictate");
			dictationButton.setEnabled(false);
			break;
		case TYPE:
			final TextView resultLabel = (TextView) myActivity.findViewById(R.id.resultLabel);
			resultLabel.setText(message);
			break;
		case HINT:
			final TextView resultHintLabel = (TextView) myActivity.findViewById(R.id.resultHintLabel);
			resultHintLabel.setText(message);
			break;
		default:
			break;
		}
	}

	private void displayAndPlayErrorMessage(Views currentFragment, InterpretationError data){
		switch(currentFragment){
		case SAY:
			final Button listenButton = (Button) myActivity.findViewById(R.id.listenButton);
			listenButton.setText("Listen");
			listenButton.setEnabled(false);
			
			final TextView resultSayLabel = (TextView) myActivity.findViewById(R.id.resultSayLabel);
			resultSayLabel.setText(data.reason.name() + ". " + data.message);

			MMFController.getInstance().playPrompt(data.reason.name()+ ". " + data.message, PromptType.TEXT);
			break;
		case DICTATE:
			final Button dictationButton = (Button) myActivity.findViewById(R.id.dictationButton);
			dictationButton.setText("Dictate");
			dictationButton.setEnabled(false);
			
			final TextView resultDictationLabel = (TextView) myActivity.findViewById(R.id.resultDictationLabel);
			resultDictationLabel.setText(data.reason.name() + ". " + data.message);

			MMFController.getInstance().playPrompt(data.reason.name()+ ". " + data.message, PromptType.TEXT);
			break;
		case TYPE:
			final EditText typeText = (EditText) myActivity.findViewById(R.id.typeText);
			MMFController.getInstance().playPrompt("Error in Interpretation, I couldn't understand what you meant by "+ typeText.getText(), PromptType.TEXT);

			final TextView resultLabel = (TextView) myActivity.findViewById(R.id.resultLabel);
			resultLabel.setText(data.message);
			break;
		case HINT:
			final Spinner hintSpinner = (Spinner) myActivity.findViewById(R.id.hintSpinner);
			final String text = hintSpinner.getItemAtPosition(hintSpinner.getSelectedItemPosition()).toString();
			MMFController.getInstance().playPrompt("Error in Interpretation, I couldn't understand what you meant by "+ text, PromptType.TEXT);

			final TextView resultHintLabel = (TextView) myActivity.findViewById(R.id.resultHintLabel);
			resultHintLabel.setText(data.message);
			break;
		default:
			break;
		}
	}
}