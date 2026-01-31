# Babble

To put it simply, Babble is a privacy-oriented translation LLM wrapper that supports horizontal, vertical, stylized, and typed Japanese text (with other languages to come). Written in Java, it utilizes JavaFX, Tesseract, OCR, and LMStudio APIs for local transcription and translation of text from images
captured on your device that STAY on your device.  

See examples at the end of this file.

## Installation
1. Download the latest release
  1. If you're on Mac or Linux you'll use the .jar file, please download Java JDK 25 before using it (https://www.oracle.com/java/technologies/downloads/#java25).
3. Download LMStudio (set your understanding to developer)
4. Download an llm (in the Discover tab) for translation and maybe one for transcription (see the recommendations)
5. Move to the developer tab and copy the link next to "reachable at:"
6. Above that you should see "Select a model to load", click it and pick the model(s) you'd like to use
7. Take note of or copy the llm tags somewhere
8. Open Babble and click the gear icon
9. Paste in the link in the "API Endpoint URL"
10. Paste in the model tags for translation (and vision if you have one)
11. Press "Save & Close" then start scanning with the play button

## Settings

### Image Processing Modes
- **Native** >  
Passes captured screenshot as is.
- **Upscaled** >  
Doubles the resolution of an image. Helpful for small vn sections (text boxes).
- **Letterboxed** >  
Provides borders so images maintain a square dimension for vision llms that LOVE to hallucinate on odd image dimensions.
- **Auto** >  
Swaps between the past 3 modes based on the image. 
I'd use this most of the time unless you wanna see if your llm performs better with either of these modes
with a specific scenario.

### OCR Modes
- **Document Mode** >  
Uses Tesseract OCR. Best for clean, typed, documents/text. I wouldn't use this unless you either have the lowest
of low hardware and cannot afford to spend any resources on the next two modes. *It struggles with game backgrounds and styled fonts/writing*.
- **AI Vision** >
Uses a vision LLM to transcribe all the text in an image.
Really great for complex backgrounds and odd looking writing/fonts.
*Doesn't position text on top of untranslated text and is more resource intensive if you want it fast (you need both models in memory)*.
- **Paddle** >
Uses PaddleOCR with a shifting algo that slightly shifts images and combines the result, overall reducing garbage output.
Works great for doing quick transcription of game UI elements and more stylized text.
*This mode is more resource intensive, but generally the best option (when considering the requirements to run it).*
- **Hybrid** >
Top tier quality mode. Combines PaddleOCR to find text and group them into boxes,
crops those boxes then arranges them into a vertical stack, and sends that stack to the Vision LLM to read text.
You WILL get the best translation from this mode and if you have good hardware I'd recommend using this mode over the others, unless you're reading
a visual novel (I'd use just plain AI Vision). 
*This mode is really resource intensive and relies on two prompts to LLMs, IT WILL NOT GO FAST ON THE WRONG HARDWARE, wouldn't recommend using this one (unless you NEED TO) if you have anything less than
12gb vram (still pushing it) and 32gb DDR5.*

## Model Recommendations
If you have a model to recommend please make an issue with tests (image you used) and the results.
### Vision
- huihui-minicpm-v-4_5-abliterated: Works great in general AI Vision and Hybrid mode (compared to 2.6). I used it for all of my testing.
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
After searching for a while to get some kind of overlay, that didn't send whatever I was reading off to a server, I was kinda empty-handed: solutions either didn't work for UI elements, 
didn't overlay on the text (transcribed) properly, or only sent images of my screen to some AI on a server.

Fast-forward a few weeks into winter break, I was looking at my options for a cool project to populate my dead/dying Github page, so I decided that this would be the one since I'd probably actually use it and I've been obsessed with local LLMs.

That being said, I really wished developers would pay for translations more often overseas. I understand that most of these companies over there don't have the resources to provide good translations for
their 5 English speaking players, I just hope that going forward some game/company sets the standard of providing English translation, as I realized throughout this project that
AI for translation typically isn't accurate (even on large proprietary models) as the AI is typically incapable of conveying the author's original meaning/feelings with a piece.

## Examples
Here are some examples using huihui-minicpm-v-4_5-abliterated, minicpm-v-2.6, Tesseract OCR, and Paddle OCR for detection with qwen/qwen2.5-vl-7b for translation of all text.  

These are smaller models and these translations do show the limits of what consumer hardware can do in a reasonable time. If you feed the text to a larger translator most issues like regressed meaning and additional words (due to misinterpretation) start to go away, but will still be present (due to the nature of llms). This is my 2nd little warning to only use this for translation of 
unimportant things and to never 100% trust an LLM for a translation.

<img src="./exampleImages/Hybrid%20Mode.png" alt="Hybrid Mode">
<img src="./exampleImages/Hybrid%20Mode%20Vertical.png" alt="Hybrid Mode">
<img src="./exampleImages/AI%20Vision.png" alt="AI Vision">
<img src="./exampleImages/document%20mode.png" alt="Tess OCR">
<img src="./exampleImages/Paddle%20OCR.png" alt="Paddle OCR">

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
