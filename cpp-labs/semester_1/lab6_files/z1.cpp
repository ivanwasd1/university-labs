#include <fstream>
#include <iostream>
#include <string>

const int MAX_SIZE = 1000;

struct word {
    int cnt = 0;
    std::string s;
};

word mostCounter(const std::string* arr, int n) {
    if (n <= 0)
        throw "File contains no words";

    word best{0, arr[0]};

    for (int i = 0; i < n; i++) {
        word now{0, arr[i]};

        for (int j = 0; j < n; j++) {
            if (arr[j] == now.s)
                now.cnt++;
        }

        if (best.cnt < now.cnt)
            best = now;
    }

    return best;
}

int main() {
    try {
        std::ifstream in("input.txt");
        if (!in.is_open())
            throw "We can't open file";

        std::string arrStr[MAX_SIZE];
        int arrSize = 0;

        std::string temp;
        while (in >> temp) {
            if (arrSize >= MAX_SIZE)
                throw "Too many words in file";
            arrStr[arrSize++] = temp;
        }

        if (arrSize == 0)
            throw "File contains no words";

        word counter = mostCounter(arrStr, arrSize);

        std::cout << "Most common word: " << counter.s << '\n';
        std::cout << "Count: " << counter.cnt << '\n';
    }
    catch (const char* error) {
        std::cout << "error - " << error << '\n';
    }

    return 0;
}
