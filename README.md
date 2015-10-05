# Genesis
A simple artificial intelligence program written in Java. It starts with no knowledge, but learns as you talk to it. I admit, it's not the best AI in the world, but I got bored one day.

# Installation
After downloading, compile it into an executable JAR file. From the command line, run the JAR using the following:  

    java -jar Genesis.jar

You are now able to talk to the Genesis AI.

# Conversation

Genesis is limited in its understanding, so you must follow these rules:

* Your first message must be a greeting. (Examples: `hello`, `hi`)
  * Genesis will respond with a greeting that it knows. On the first run, it will be identical to what you say.
* Supported conversation input values:
  * Laughing
    * Detected if >=50% of the message is composed of `h` or `l` characters. (Examples: `lol`, `haha`)
  * Setting values
    * Values are set using this syntax: `xxx is yyy`. (Example: `Your name is Genesis.`)
  * Retrieving values
    * Values are retrieved using this syntax: `what is xxx` (Example: `What is your name?`)
    * If no value entry is found, and there is an internet connection, Genesis will check Wikipedia for an answer.
      * If it finds a valid article, it will also save it to reduce lookup time in the future for similar questions.
  * If anything outside this list is given, Genesis will not respond
* Your last message must be a farewell. (Examples: `goodbye`, `see you later`)
  * If the farewell is unfamiliar to Genesis, you must type in `exit` to notify Genesis that the conversation is over.
  * Genesis will then respond with a farewell that it knows. On the first run, it will be identical to what you said before typing `exit`.

By talking to Genesis while respecting this behavior, the program will function properly.

# Feedback

Feedback is very much appreciated. Pull requests are great because I don't really have time to always work on Genesis. Issues are very helpful for finding bugs and suggesting features. Thanks!
