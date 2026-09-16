#include <gtest/gtest.h>

int solve(int num);

TEST(DeleteDigitsTest, NoEvenCounts) {
    int result = solve(1234);
    int expected = 1234;
    EXPECT_EQ(result, expected);
}

TEST(DeleteDigitsTest, OneDigitEvenCount) {
    int result = solve(1223);
    int expected = 13;
    EXPECT_EQ(result, expected);
}

TEST(DeleteDigitsTest, AllDigitsDetele) {
    int result = solve(1122);
    int expected = -1;
    EXPECT_EQ(result, expected);
}

TEST(DeleteDigitsTest, ALotOfEvenDigitsCount) {
    int result = solve(1122334);
    int expected = 4;
    EXPECT_EQ(result, expected);
}

TEST(DeleteDigitsTest, ZerosEvenCount) {
    int result = solve(1010);
    int expected = -1;
    EXPECT_EQ(result, expected);
}

TEST(DeleteDigitsTest, MixedDigits) {
    int result = solve(1231231);
    int expected = 111;
    EXPECT_EQ(result, expected);
}

TEST(DeleteDigitsTest, SingleDigitEvenCount) {
    int result = solve(11);
    int expected = -1;
    EXPECT_EQ(result, expected);
}

TEST(DeleteDigitsTest, SingleDigitOddCount) {
    int result = solve(7);
    int expected = 7;
    EXPECT_EQ(result, expected);
}

TEST(DeleteDigitsTest, AllDigitsEvenBigNumber) {
    int result = solve(98766789);
    int expected = -1;
    EXPECT_EQ(result, expected);
}

TEST(DeleteDigitsTest, NonNaturalInputANDExit) {
    EXPECT_EXIT(solve(0), ::testing::ExitedWithCode(1), "");
}
