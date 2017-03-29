# FRC-Match-Predictor
Uses Neural Networks to predict FRC 2017 matches.

This uses the Neural Network framework I made in my [Java-Simple-Neural-Network](https://github.com/coolioasjulio/Java-Simple-Neural-Network) project.

I haven't created a GUI or anything for it, just tests to train it. The training data has been gotten from the TBA API, and is saved to data.dat in JSON format.

This project uses GSON to access the JSON from the TBA, and to serialize the training data.

Eventually, I'll create a lookup system where by inputting 6 teams it will tell you the expected outcome.
