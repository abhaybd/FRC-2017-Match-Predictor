# FRC-Match-Predictor
Uses Neural Networks to predict FRC 2017 matches.

This uses the Neural Network framework I made in my [Java-Simple-Neural-Network](https://github.com/coolioasjulio/Java-Simple-Neural-Network) project.

I haven't created a GUI or anything for it, just tests to train it. The training data has been gotten from the TBA API, and is saved to data.dat in JSON format.

This project uses GSON to access the JSON from the TBA, and to serialize the training data.

## How to use this thing
Just run MatchPredictor. It will ask you if you want to train again with updated data. If you say yes, it will retrain. This requires internect access, and will take a while.

After that, or if you say no, it will ask for the blue alliance members. Input the team number preceded by 'frc'. Ex: 'frc420'.

Then, input the red alliance members. If possible for both, input in the order that FIRST provides them in in the match list.

It will crunch some numbers for a couple seconds, and then spit out some numbers. The first number is the probability of blue winning. The second number is the probability of red winning. The third number is the probability of a tie.

Remember, this uses machine learning, and is a work in progress, so don't take the results to be definite.
