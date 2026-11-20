#!/usr/bin/env python3
"""
================================================================================
Project: Library Book Lending and Usage Analysis System (Scenario 4)
Academic Year: 2026-2027 (Odd Semester) | Class: 25IT3A
Course: 25ITC31 - Foundations of Data Science
Institution: Velalar College of Engineering and Technology, Erode - 12
Department: Department of Information Technology
Student: Abishek (Register No: 732925ITR004)
================================================================================

Description:
This script extracts historical book lending transactions from the SQLite
database (library.db), cleans and preprocesses the circulation data, performs
exploratory data analysis (EDA), and generates 4 publication-quality visualizations
saved in the visualizations/ directory.
"""

import os
import sqlite3
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

# Configure visual styling
sns.set_theme(style="whitegrid", palette="muted")
plt.rcParams["font.sans-serif"] = "DejaVu Sans"
plt.rcParams["figure.autolayout"] = True

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_DIR = os.path.dirname(BASE_DIR)
DB_PATH = os.path.join(PROJECT_DIR, "database", "library.db")
SCHEMA_PATH = os.path.join(PROJECT_DIR, "database", "schema.sql")
SEED_PATH = os.path.join(PROJECT_DIR, "database", "seed_data.sql")
VIS_DIR = os.path.join(BASE_DIR, "visualizations")


def ensure_database_ready():
    """Ensures SQLite database exists and is populated with schema and seed data."""
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    os.makedirs(VIS_DIR, exist_ok=True)

    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()

    # Check if tables exist
    cursor.execute("SELECT count(name) FROM sqlite_master WHERE type='table' AND name='transactions';")
    if cursor.fetchone()[0] == 0:
        print("[INFO] Database not found or empty. Bootstrapping schema and seed data...")
        with open(SCHEMA_PATH, "r", encoding="utf-8") as f:
            cursor.executescript(f.read())
        with open(SEED_PATH, "r", encoding="utf-8") as f:
            cursor.executescript(f.read())
        conn.commit()
        print("[SUCCESS] Database initialized successfully at:", DB_PATH)
    else:
        print("[INFO] Connected to existing SQLite database at:", DB_PATH)

    conn.close()


def extract_data():
    """Extracts circulation data from the database using SQL join view."""
    conn = sqlite3.connect(DB_PATH)
    query = """
    SELECT 
        transaction_id,
        issue_date,
        due_date,
        return_date,
        status,
        fine_amount,
        student_id,
        register_no,
        student_name,
        department,
        year,
        book_id,
        isbn,
        book_title,
        category_name,
        author_name
    FROM vw_circulation_analytics;
    """
    df = pd.read_sql_query(query, conn)
    conn.close()
    return df


def preprocess_data(df):
    """
    Performs data cleaning, type conversion, feature engineering,
    and missing value handling (Course: 25ITC31 Units 3 & 4).
    """
    print("\n" + "=" * 70)
    print(" DATA PREPROCESSING & WRANGLING")
    print("=" * 70)
    print(f"Raw records retrieved: {len(df)}")

    # 1. Date conversions (ISO format to datetime64)
    df["issue_date"] = pd.to_datetime(df["issue_date"])
    df["due_date"] = pd.to_datetime(df["due_date"])
    df["return_date"] = pd.to_datetime(df["return_date"])

    # 2. Reference evaluation date for active loans
    current_reference_date = pd.Timestamp.now().normalize()

    # 3. Compute actual or interim return date
    df["effective_return_date"] = df["return_date"].fillna(current_reference_date)

    # 4. Feature Engineering: Loan Duration (in days)
    df["loan_duration_days"] = (df["effective_return_date"] - df["issue_date"]).dt.days

    # 5. Feature Engineering: Overdue Days
    df["overdue_days"] = (df["effective_return_date"] - df["due_date"]).dt.days
    df["overdue_days"] = df["overdue_days"].apply(lambda x: max(0, x))

    # 6. Time dimensions
    df["borrow_year"] = df["issue_date"].dt.year
    df["borrow_month"] = df["issue_date"].dt.month
    df["borrow_month_name"] = df["issue_date"].dt.strftime("%b")
    df["borrow_year_month"] = df["issue_date"].dt.strftime("%Y-%m")
    df["day_of_week"] = df["issue_date"].dt.day_name()

    # 7. Fine verification (₹2.00 / day)
    df["calculated_fine"] = df["overdue_days"] * 2.0

    print("[INFO] Preprocessing complete. Derived features added:")
    print("       - loan_duration_days, overdue_days, borrow_year_month, day_of_week")
    return df


def plot_category_distribution(df):
    """
    Visualization 1: Borrowing patterns by Subject Category
    (Bar Chart + Percentage Donut Chart)
    """
    print("[1/4] Generating Category Distribution Visualizations...")
    cat_counts = df["category_name"].value_counts().reset_index()
    cat_counts.columns = ["category_name", "borrow_count"]

    fig, axes = plt.subplots(1, 2, figsize=(16, 6))

    # 1. Bar Chart
    colors = sns.color_palette("crest", len(cat_counts))
    sns.barplot(
        x="borrow_count",
        y="category_name",
        data=cat_counts,
        ax=axes[0],
        palette=colors
    )
    axes[0].set_title("Total Borrowing Transactions by Category", fontsize=14, fontweight="bold")
    axes[0].set_xlabel("Number of Times Borrowed", fontsize=12)
    axes[0].set_ylabel("Book Category", fontsize=12)

    for i, val in enumerate(cat_counts["borrow_count"]):
        axes[0].text(val + 0.1, i, str(val), va="center", fontsize=10, fontweight="bold")

    # 2. Donut Chart
    axes[1].pie(
        cat_counts["borrow_count"],
        labels=cat_counts["category_name"],
        autopct="%1.1f%%",
        startangle=140,
        colors=colors,
        wedgeprops=dict(width=0.4, edgecolor="white", linewidth=2)
    )
    axes[1].set_title("Category Share of Total Circulation", fontsize=14, fontweight="bold")

    plt.suptitle("Scenario 4: Library Category-Wise Circulation Analysis", fontsize=16, fontweight="bold")
    save_path = os.path.join(VIS_DIR, "category_distribution.png")
    plt.savefig(save_path, dpi=300, bbox_inches="tight")
    plt.close()
    print(f"      Saved: {save_path}")


def plot_monthly_trends(df):
    """
    Visualization 2: Monthly Borrowing Volume & Trends Over Time
    (Line Chart with Area Fill and Overdue Annotations)
    """
    print("[2/4] Generating Monthly Borrowing Trend Visualizations...")
    monthly = df.groupby("borrow_year_month").agg(
        total_loans=("transaction_id", "count"),
        returned_loans=("return_date", "count"),
        overdue_cases=("overdue_days", lambda x: (x > 0).sum()),
        total_fine=("fine_amount", "sum")
    ).reset_index()

    fig, ax1 = plt.subplots(figsize=(12, 6))

    # Line plot for monthly borrowings
    ax1.plot(monthly["borrow_year_month"], monthly["total_loans"], marker="o", color="#1f77b4",
             linewidth=3, markersize=8, label="Total Monthly Loans")
    ax1.fill_between(monthly["borrow_year_month"], monthly["total_loans"], color="#1f77b4", alpha=0.15)

    # Bar overlay for late returns / overdue cases
    ax1.bar(monthly["borrow_year_month"], monthly["overdue_cases"], color="#e74c3c", alpha=0.5,
            width=0.4, label="Overdue Returns (Fine Applied)")

    ax1.set_title("Monthly Book Borrowing Volume & Overdue Incident Trends (2026)", fontsize=14, fontweight="bold")
    ax1.set_xlabel("Year - Month", fontsize=12)
    ax1.set_ylabel("Number of Transactions", fontsize=12)
    ax1.tick_params(axis="x", rotation=30)
    ax1.grid(True, linestyle="--", alpha=0.5)

    for i, row in monthly.iterrows():
        ax1.annotate(f"{row['total_loans']}", (row["borrow_year_month"], row["total_loans"] + 0.15),
                     ha="center", fontsize=9, fontweight="bold", color="#1f77b4")

    ax1.legend(loc="upper left")

    save_path = os.path.join(VIS_DIR, "monthly_borrowing_trends.png")
    plt.savefig(save_path, dpi=300, bbox_inches="tight")
    plt.close()
    print(f"      Saved: {save_path}")


def plot_top_books(df):
    """
    Visualization 3: Most Frequently Borrowed Books & Author Ranking
    """
    print("[3/4] Generating Top Borrowed Books Visualizations...")
    book_counts = df.groupby(["book_title", "category_name", "author_name"]).agg(
        borrow_count=("transaction_id", "count")
    ).reset_index().sort_values(by="borrow_count", ascending=True)

    fig, ax = plt.subplots(figsize=(13, 7))

    palette = sns.color_palette("viridis", len(book_counts))
    bars = ax.barh(book_counts["book_title"], book_counts["borrow_count"], color=palette)

    ax.set_title("Most Frequently Borrowed Book Titles (Library Circulation Index)", fontsize=14, fontweight="bold")
    ax.set_xlabel("Number of Times Borrowed", fontsize=12)
    ax.set_ylabel("Book Title", fontsize=12)
    ax.grid(axis="x", linestyle="--", alpha=0.5)

    for bar in bars:
        width = bar.get_width()
        ax.text(width + 0.1, bar.get_y() + bar.get_height() / 2, f"{int(width)}",
                va="center", fontsize=10, fontweight="bold")

    save_path = os.path.join(VIS_DIR, "top_borrowed_books.png")
    plt.savefig(save_path, dpi=300, bbox_inches="tight")
    plt.close()
    print(f"      Saved: {save_path}")


def plot_overdue_and_fines(df):
    """
    Visualization 4: Overdue Return Durations and Fine Distribution
    """
    print("[4/4] Generating Overdue and Fine Distribution Visualizations...")
    fig, axes = plt.subplots(1, 2, figsize=(15, 6))

    # Filter transactions that had overdues
    overdue_df = df[df["overdue_days"] > 0]

    # Subplot 1: Distribution of Overdue Days
    sns.histplot(
        overdue_df["overdue_days"],
        bins=6,
        kde=True,
        color="#e67e22",
        ax=axes[0]
    )
    axes[0].set_title("Distribution of Overdue Days for Late Returns", fontsize=13, fontweight="bold")
    axes[0].set_xlabel("Overdue Days past 14-day limit", fontsize=11)
    axes[0].set_ylabel("Frequency", fontsize=11)

    # Subplot 2: Fine Amount Boxplot by Category
    sns.boxplot(
        x="fine_amount",
        y="category_name",
        data=df[df["fine_amount"] > 0],
        ax=axes[1],
        palette="Set2"
    )
    axes[1].set_title("Fine Amount Distribution by Category (₹2/day)", fontsize=13, fontweight="bold")
    axes[1].set_xlabel("Fine Paid in INR (₹)", fontsize=11)
    axes[1].set_ylabel("Subject Category", fontsize=11)

    plt.suptitle("Overdue Duration & Penalty Fine Diagnostics", fontsize=15, fontweight="bold")
    save_path = os.path.join(VIS_DIR, "overdue_and_fine_analysis.png")
    plt.savefig(save_path, dpi=300, bbox_inches="tight")
    plt.close()
    print(f"      Saved: {save_path}")


def display_statistical_summary(df):
    """Prints comprehensive descriptive statistics (Course: 25ITC31 Unit 1 & 5)."""
    print("\n" + "=" * 70)
    print(" DESCRIPTIVE STATISTICAL SUMMARY (Foundations of Data Science)")
    print("=" * 70)
    print(f"Total Transactions Recorded  : {len(df)}")
    print(f"Completed Returns             : {(df['status'] == 'RETURNED').sum()}")
    print(f"Currently Active Loans        : {(df['status'] == 'ISSUED').sum()}")
    print(f"Late Returns / Overdue Count  : {(df['overdue_days'] > 0).sum()} ({((df['overdue_days'] > 0).sum()/len(df))*100:.1f}%)")
    print(f"Total Overdue Fines Collected : ₹{df['fine_amount'].sum():.2f}")
    print(f"Average Fine per Late Loan    : ₹{df[df['fine_amount'] > 0]['fine_amount'].mean():.2f}")
    print(f"Average Loan Duration         : {df['loan_duration_days'].mean():.1f} days")
    print("-" * 70)
    print("Top 3 Most Popular Categories:")
    for cat, cnt in df["category_name"].value_counts().head(3).items():
        print(f"  * {cat:<28} : {cnt} loans")
    print("-" * 70)
    print("Top 3 Most Borrowed Titles:")
    for title, cnt in df["book_title"].value_counts().head(3).items():
        print(f"  * {title:<40} : {cnt} loans")
    print("=" * 70 + "\n")


def main():
    print("=" * 70)
    print(" VCET IT - LIBRARY USAGE ANALYSIS & DATA SCIENCE PIPELINE")
    print(" Student: Abishek (732925ITR004) | Class: 25IT3A")
    print("=" * 70)

    # 1. Database check and load
    ensure_database_ready()

    # 2. Extract
    df_raw = extract_data()

    # 3. Clean & Preprocess
    df_clean = preprocess_data(df_raw)

    # 4. Generate Visualizations
    plot_category_distribution(df_clean)
    plot_monthly_trends(df_clean)
    plot_top_books(df_clean)
    plot_overdue_and_fines(df_clean)

    # 5. Summarize
    display_statistical_summary(df_clean)

    print("[SUCCESS] All 4 visualization artifacts successfully generated in:")
    print("          ", VIS_DIR)


if __name__ == "__main__":
    main()
