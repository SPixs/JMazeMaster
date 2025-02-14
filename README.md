# 🏰 JMazeMaster - Relive the Commodore 64 Experience!

JMazeMaster is a Java implementation of the classic **Maze Master** game, originally released on the **Commodore 64**. This version aims to be as faithful as possible to the original, without using emulation. The complete game logic has been reconstructed in Java by disassembling the binary code from the original cartridge.

## 🎮 Game Overview

Embark on a perilous adventure through dark corridors and hidden passageways, ever wary of lurking monsters that could strike at any moment. In **Maze Master**, you command a party of three characters (wizards and warriors), minimally armed but capable of gaining strength, gold, and powerful magic as they overcome the dangers of the labyrinth. As you progress through the maze, you will uncover clues to a mystical riddle that must be solved to penetrate the final chamber of the **BALROG**, a formidable foe determined to destroy the liege and his realm. Victory is achieved when the BALROG is defeated, and the riddle is solved, restoring peace to the kingdom.

![Game Screenshot](media/main_menu.png) ![Image](media/enter_maze.png)

## 🛠️ Installation and Compilation

### 📌 Prerequisites
Before getting started, make sure you have installed:
- [Java 11+](https://adoptium.net/)
- [Maven](https://maven.apache.org/)

### 🚀 Installation
Clone the Git repository and navigate to the project directory:
```sh
git clone https://github.com/SPixs/JMazeMaster.git
cd JMazeMaster
```

### 🏗️ Compilation with Maven
To compile the project, use:
```sh
mvn clean package
```
This generates an executable file in `target/JMazeMaster-1.0-SNAPSHOT.jar`.

### 🎲 Execution
#### 🔹 With Maven (quick development run)
```sh
mvn exec:java
```

#### 🔹 Running the Generated JAR
```sh
java -jar target/JMazeMaster-1.0-SNAPSHOT.jar
```

## 📜 License
This project is distributed under the **MIT** license. See the [LICENSE](LICENSE) file for more information.

## 📷 More Game Screenshots
![Image](media/fight.png)

---
⭐ **If you like this project, don't forget to give it a star on GitHub!** ⭐
