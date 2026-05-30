#!/bin/bash
# Build script for Smart City Recommendation System

echo "Building Smart City Recommendation System..."
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not installed or not in PATH."
    echo "Please install Maven from: https://maven.apache.org/download.cgi"
    exit 1
fi

# Build the project
echo "Running Maven build..."
mvn clean package

if [ $? -eq 0 ]; then
    echo ""
    echo "BUILD SUCCESSFUL!"
    echo ""
    echo "To run the application, execute:"
    echo "java -jar target/SmartCityRecommender.jar"
    echo ""
else
    echo ""
    echo "BUILD FAILED!"
    echo "Please check the error messages above."
    exit 1
fi
