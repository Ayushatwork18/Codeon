# Codeon

Codeon is an online platform for learning Python by actually writing code. You pick a problem, write your solution in the browser, run it, and find out straight away whether it is correct.

It was built as a Core Java project, using Servlets for the web layer and JDBC to talk to a MySQL database.

---

## About the Project

Most beginners learn Python by reading, but programming is learned by doing. Codeon gives learners a place to practice small problems step by step, from simple input and output up to loops, strings, lists and dictionaries, and see instant feedback on every attempt.

The platform has three kinds of users, each with their own dashboard, so a whole class can use it together.

## Features

### For Students
- Practice questions split into **Basic**, **Medium** and **Advanced** sections
- 21 beginner questions included to start with
- Write and run Python code directly in the browser
- Instant result: correct, wrong output, syntax error or time limit exceeded
- The model solution unlocks only after you have tried the question at least once
- A progress page showing how many questions you have solved, with a reset button

### For Instructors
- Create new practice exercises with a description, sample input and expected output
- See whether each exercise is pending, approved or rejected

### For Admins
- Approve or reject exercises sent by instructors
- View and manage users
- See an overview of activity on the platform

## How Your Code Is Checked

When a student presses Run, the server first checks the code for syntax errors, then runs it with a time limit so an endless loop cannot freeze the site. The program's output is compared with the expected output of the question, and the attempt is saved so progress can be tracked.

## Technologies Used

- **Java 17** and **Jakarta Servlets** for the backend
- **JDBC** with **MySQL 8** for the database
- **HTML, CSS and JavaScript** for a responsive single-page frontend with dark mode
- **Maven** for building and **Jetty** for running the site
- **Python 3** to run the submitted code

## Languages Used

Share of the project's source code:

- Java: 46.9%
- JavaScript: 30.8%
- CSS: 14.7%
- HTML: 3.0%
- SQL: 2.6%
- PowerShell: 2.0%

---

## Before You Run It

Install these and check that each one works in a terminal:

- **Java JDK 17 or newer** (`java -version`)
- **Maven 3.9 or newer** (`mvn -version`)
- **Python 3.8 or newer** (`python --version`)
- **MySQL 8** installed and running (you can check by opening MySQL Workbench)

## Setup

**1. Download the project**

```powershell
git clone https://github.com/Ayushatwork18/Codeon.git
cd Codeon
```

**2. Create the database**

```powershell
Get-Content schema.sql | mysql -u root -p
```

If `mysql` is not recognised, open `schema.sql` in MySQL Workbench and run it there instead.

## Run the Project

From inside the `Codeon` folder, paste this single line into PowerShell. It asks for your MySQL password, starts the project, and opens it in your browser:

```powershell
$env:DB_PASSWORD = [Net.NetworkCredential]::new('', (Read-Host 'Enter MySQL password' -AsSecureString)).Password; try { mvn "org.eclipse.jetty.ee10:jetty-ee10-maven-plugin:12.0.14:run" | ForEach-Object { $_; if ($_ -match 'Started ServerConnector') { Write-Host "`n>>> WEBSITE READY: http://localhost:8080/ <<<`n" -ForegroundColor Green; Start-Process "http://localhost:8080/" } } } finally { Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue }
```

The first run downloads a few files, so give it a minute. When you see **WEBSITE READY**, the site is open at http://localhost:8080/. Press `Ctrl + C` to stop it.

Your password is typed hidden and is never saved in any file.

**On Mac or Linux**, use these commands instead, and first change `python.cmd=python` to `python3` in `src/main/resources/db.properties`:

```bash
read -rsp "MySQL password: " DB_PASSWORD; export DB_PASSWORD
mvn org.eclipse.jetty.ee10:jetty-ee10-maven-plugin:12.0.14:run
```

## Demo Logins

These accounts are created automatically the first time the project starts:

- **Student:** student@demo.com / student123
- **Instructor:** teacher@demo.com / teacher123
- **Admin:** admin@demo.com / admin123

They are for local testing only. Change them before putting the site online.

## Project Structure

```
Codeon/
├── schema.sql              database tables
├── pom.xml                 Maven build file
└── src/main/
    ├── java/               servlets, database access, Python runner
    ├── resources/          settings and the 21 practice questions
    └── webapp/index.html   the whole frontend
```

## Good to Know

Student code runs directly on your computer with a time limit but no sandbox, so don't put this project on the public internet as it is.
