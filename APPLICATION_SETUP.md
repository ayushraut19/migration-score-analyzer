# Project Setup Instructions

## ✅ Installation & Setup

### Prerequisites
- **Java**: Java 11 or higher ([Download](https://www.oracle.com/java/technologies/downloads/))
- **Maven**: Maven 3.6+ ([Download](https://maven.apache.org/download.cgi))
- **Git** (Optional): For version control ([Download](https://git-scm.com/))

### Verify Prerequisites
```bash
# Check Java
java -version

# Check Maven
mvn -version
```

### Output Example (Java)
```
java version "11.0.15" 2022-04-19 LTS
Java(TM) SE Runtime Environment 18.9
```

## 🚀 Quick Setup (Windows)

### Step 1: Navigate to Project
```cmd
cd Desktop
cd "migration score analyzer"
```

### Step 2: Build
```cmd
build.bat
```

### Step 3: Run
```cmd
java -jar target/SmartCityRecommender.jar
```

## 🐧 Quick Setup (Linux/Mac)

### Step 1: Navigate to Project
```bash
cd ~/Desktop
cd "migration score analyzer"
```

### Step 2: Make Build Script Executable
```bash
chmod +x build.sh
```

### Step 3: Build
```bash
./build.sh
```

### Step 4: Run
```bash
java -jar target/SmartCityRecommender.jar
```

## 🔧 Manual Build with Maven

```bash
# Clean and build
mvn clean package

# Run directly
java -jar target/SmartCityRecommender.jar
```

## 💻 IDE Setup

### IntelliJ IDEA
1. Open IntelliJ IDEA
2. File → Open → Select project folder
3. Select "Open as Project"
4. Wait for indexing
5. Right-click `ApplicationLauncher.java`
6. Run "ApplicationLauncher.main()"

### Eclipse
1. File → Import → Existing Maven Projects
2. Select project root
3. Click Finish
4. Right-click project → Run As → Java Application
5. Select `ApplicationLauncher`

### VS Code
1. Install Extensions:
   - Extension Pack for Java
   - Maven for Java
2. Open folder
3. Open `ApplicationLauncher.java`
4. Click "Run" button or press F5

## 📦 Project Structure After Build

```
migration-score-analyzer/
├── src/                    # Source code
├── target/                 # Build output
│   └── SmartCityRecommender.jar
├── data/
│   ├── localities.json    # Data file
│   └── favorites.txt      # Auto-created
├── pom.xml               # Maven config
├── README.md             # Documentation
├── QUICKSTART.md         # Quick guide
├── DEVELOPMENT.md        # Developer guide
└── APPLICATION_SETUP.md  # This file
```

## 🔍 Troubleshooting

### Issue: "Java not found"
```
Error: 'java' is not recognized as an internal or external command
```
**Solution**: Download Java from oracle.com and add to PATH

### Issue: "Maven not found"
```
Error: 'mvn' is not recognized
```
**Solution**: Download Maven and add bin folder to PATH

**Windows PATH Setup**:
1. Find Maven bin folder
2. System Properties → Environment Variables
3. Add Maven path to PATH variable

### Issue: Build fails
```
[ERROR] Failed to execute goal
```
**Solution**:
```bash
mvn clean
mvn install
mvn package
```

### Issue: Port already in use (if API server is added)
```
Error: Port 8080 already in use
```
**Solution**: Change port in `application.properties`

## 🖥️ System Requirements

### Minimum
- RAM: 512 MB
- Disk Space: 500 MB
- Java: 11+

### Recommended
- RAM: 2 GB
- Disk Space: 1 GB
- Java: 11+

## 📝 Configuration

Main config file: `application.properties`

### Key Settings
```properties
# App
app.name=Smart City Recommendation System - Migration Score Analyzer
app.version=1.0.0

# UI
ui.width=1200
ui.height=800

# Logging
logging.level=INFO
```

## ✨ First Run

When you first run the application:
1. Application window opens
2. Select a city
3. Set your budget
4. Click "Get Recommendations"
5. View your personalized recommendations!

## 📚 Documentation Files

### README.md
Complete project documentation with features, architecture, and usage

### QUICKSTART.md
5-minute quick start guide for first-time users

### DEVELOPMENT.md
Developer guide with architecture details, extending features, and best practices

### APPLICATION_SETUP.md
This file - Installation and setup instructions

## 🎯 Next Steps

1. ✅ Run the application
2. 📖 Read QUICKSTART.md for usage
3. 👨‍💻 Read DEVELOPMENT.md if extending
4. 📊 Try different cities and budgets
5. 💾 Save your favorite localities

## 🆘 Support

### Common Questions

**Q: Can I add more cities?**
A: Yes! Edit `data/localities.json` and add more entries.

**Q: How do I fix scoring?**
A: Edit `ScoringEngine.java` - see DEVELOPMENT.md

**Q: Can I use a database?**
A: Yes! Create database layer - see DEVELOPMENT.md

**Q: How do I deploy this?**
A: Create executable JAR with `mvn clean package`

---

**Ready to find your perfect city? Let's go! 🚀**
