# Babble

## Overview
Babble is a Java program utilizing JavaFX, Tesseract, (Paddle) OCR, and LMStudio APIs for local transcription and translation of text from images. 
Babble currently supports (horizontal or vertical) Kanji in many handwritten/typed styles, all while remaining relatively simple to use and local on your network/pc.

## Installation
1. Download the latest release
2. Download LMStudio (set your understanding to developer)
3. Download an llm (in the Discover tab) for translation and maybe one for transcription (see the recommendations)
4. Move to the developer tab and copy the link next to "reachable at:"
5. Above that you should see "Select a model to load", click it and pick the model(s) you'd like to use
6. Take note of or copy the llm tags somewhere
7. Open Babble and click the gear icon
8. Paste in the link in the "API Endpoint URL"
9. Paste in the model tags for translation (and vision if you have one)
10. Press "Save & Close" then start scanning with the play button

## Model Recommendations
If you have a model to recommend please make an issue with tests (image you used) and the results.
### Vision
- huihui-minicpm-v-4_5-abliterated: Works fine for transcription of text in Hybrid mode, but is horrible for general Vision. I used it for all of my testing, it really only excels at being small and reading small screen sections.
### Translation
- qwen/qwen2.5-vl-7b: Good enough for small pieces of text like signs and UI.
- mistral-small-3.2-24b-instruct-2506: Good for story/character dialogue. Will sometimes output something weird that you'll have to interpret yourself, but is generally good enough.
- shisa-v2-qwen2.5-32b: Better than mistral-small for translation, but still kinda has the same problems. If you give it context it works pretty well when adding small details to dialogue.
- suzume-llama-3-8b-multilingual: Okay for small/short dialogue, other than that it's not great.

## Feature Support
Everything, but windows constant scanning, should work between Windows, Java, and Linux. All of my testing has been exclusive to windows though.  
Please make an issue if you find problems on either of the platforms, I can do some testing and bug fixing on either if needed.
> I do plan on adding some version of the constant scanning to Linux and Mac later (as I hate pressing that button multiple times when using Fedora or mbp), but I've been struggling to get the same quality as on Windows. Windows has a specific API to hide elements from screen capture and I'm not aware of any on linux/mac. The only other solution I've tried is 'flickering' the text visibility to take captures, but it looks horrible.

## Build / Run

To build the project, run:

```bash
mvn clean package
```

This will generate two files in the `target` directory:
- `Babble-1.0-SNAPSHOT-fat.jar`: A runnable JAR file w/ all dependencies.
- `Babble.exe`: Windows executable.

To run the application, use:

```bash
mvn javafx:run
```

## Backstory
A while ago I ran into this jpn superhero game that I REALLY loved the character design for, but when starting it I realized that it had no English translations.
After searching for a while to get some kind of overlay, that didn't send whatever I was reading off to a server, I was kinda empty-handed as the solutions either didn't work for UI elements, didn't overlay on the text (transcribed) properly, or only sent images of my screen to some AI on a server.

Fast forward a few weeks into winter break, I was looking at my options for a cool project to populate my dead/dying Github page, so I decided that this would be the one since I'd probably actually use it and I've been obsessed with local LLMs.

## AI Usage disclosure
AI is a tool and it should be disclosed when it's being used, no matter the % of the project that it contributed to.  

### Gemini
- helped me understand tesseract functions/classes and how I would add Paddle OCR to my project with .onnx files.
- Optimize prompts used for vision and translation.

### Qwen (/qwen3-vl-30b)
- main.fxml, settings.fxml, and mStyle.css. 
  - Structure was dictated
  - The names for some elements were changed 
  - The colors were picked by me
  - Corrections were made to syntax (fxml)