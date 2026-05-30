# Quick Start Guide

## 🚀 Get Running in 5 Minutes

### Option 1: Command Line (Fastest)

#### Windows:
```bash
cd "migration score analyzer"
build.bat
java -jar target/SmartCityRecommender.jar
```

#### Linux/Mac:
```bash
cd "migration score analyzer"
chmod +x build.sh
./build.sh
java -jar target/SmartCityRecommender.jar
```

### Option 2: Using Maven Directly

```bash
mvn clean package
java -jar target/SmartCityRecommender.jar
```

### Option 3: IDE (IntelliJ IDEA / Eclipse / VS Code)

1. Open project folder in your IDE
2. Import as Maven project
3. Run `com.smartcity.view.ApplicationLauncher` as Java Application

---

## 🎯 First Steps in the App

### 1. **Select a City**
Choose from: Bangalore, Mumbai, New Delhi, or Hyderabad

### 2. **Set Your Preferences**
- **Budget**: Your annual housing budget
- **Family Size**: Number of people
- **Work Type**: On-site, Remote, or Hybrid

### 3. **Quick Profile Selection (Optional)**
Click one of these buttons to auto-adjust weights:
- **Student** 🎓
- **Bachelor** 💼
- **Family** 👨‍👩‍👧‍👦

### 4. **Fine-Tune Weights (Optional)**
Adjust importance (0-10) for:
- Job Opportunities
- Cost of Living
- Healthcare
- Transport
- Safety
- Environment
- Lifestyle

### 5. **Get Recommendations**
Click **"Get Recommendations"** button

### 6. **View Results**
See top 3 recommended localities with:
- Final composite score
- Score breakdown
- Why it's recommended for you
- View on Google Maps option

---

## 📊 Understanding Scores

### Score Range
- **8.0 - 10.0** 🟢 Excellent
- **6.0 - 8.0** 🟠 Good
- **0.0 - 6.0** 🔴 Fair

### Factors Considered
1. **Job Opportunities** - Number and quality of jobs
2. **Cost of Living** - Affordability relative to your budget
3. **Healthcare** - Hospital and medical facilities
4. **Transport** - Public transport and connectivity
5. **Safety** - Crime rates and security
6. **Environment** - Air quality and green spaces
7. **Lifestyle** - Entertainment, dining, shopping

---

## 💡 Tips & Tricks

### Smart Profiles
- **Student**: Prioritizes affordability and nightlife
- **Bachelor**: Emphasizes jobs and entertainment
- **Family**: Focuses on safety and healthcare

### Real-Time Updates
All sliders update recommendations instantly - experiment!

### Budget Tips
- **Low Budget** (₹300K-500K): Focus on Hyderabad or smaller Bangalore areas
- **Medium Budget** (₹500K-800K): Most options available
- **High Budget** (₹800K+): Premium areas in Mumbai and Bangalore

### Work Type Benefits
- **Remote Work**: Can prioritize cost and lifestyle
- **On-site**: Consider transport and office location

---

## 🐛 Troubleshooting

### App Won't Start
```
Error: Java version mismatch
Solution: Install Java 11 or higher
Check: java -version
```

### No Localities Showing
```
Error: Cannot load localities
Solution: Ensure data/localities.json exists
Check: Open data folder in explorer
```

### Build Fails
```
Error: Maven not found
Solution: Install Maven from https://maven.apache.org
Add Maven to system PATH
```

---

## 📁 Project Layout

```
migration-score-analyzer/
├── data/
│   ├── localities.json      ← Locality database
│   └── favorites.txt        ← Your saved favorites
├── src/main/java/com/smartcity/
│   ├── model/              ← Data models
│   ├── view/               ← UI screens
│   ├── controller/         ← Main logic
│   ├── service/            ← Business logic
│   └── utils/              ← Helper tools
├── target/                 ← Built JAR files
├── pom.xml                 ← Maven config
└── README.md               ← Full documentation
```

---

## 🔧 Configuration

Edit `application.properties` to customize:
- UI theme
- Logging level
- Feature toggles
- Data paths

---

## 📚 Next Steps

1. **Explore Results** - Try different cities and budgets
2. **Compare Localities** - Use comparison feature
3. **Save Favorites** - Keep track of your top picks
4. **Read Full Docs** - Check README.md for advanced features

---

## ❓ Questions?

- Check README.md for detailed documentation
- See DEVELOPMENT.md for extending the app
- Review source code comments

**Happy house hunting! 🏠**
