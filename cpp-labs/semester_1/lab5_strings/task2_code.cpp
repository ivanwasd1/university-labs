#include <iostream>
#include <string>
#include <vector>
#include <algorithm>
#include <windows.h>

bool isLetter(const char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
        || (c >= 'а' && c <= 'я') || (c >= 'А' && c <= 'Я');
}

int CountUpper(const std::string& str) {
    int cnt = 0;
    for (char ch : str) {
        if ((ch >= 'A' && ch <= 'Z') || (ch >= 'А' && ch <= 'Я')) {
            cnt++;
        }
    }
    return cnt;
}

void ChangeRegistr(std::string& str) {

    for (char& c : str) {
        if (c >= 'a' && c <= 'z')
            c = c - 'a' + 'A';
        else if (c >= 'A' && c <= 'Z')
            c = c - 'A' + 'a';
        if (c >= 'а' && c <= 'я')
            c = c - 'а' + 'А';
        else if (c >= 'А' && c <= 'Я')
            c = c - 'А' + 'а';

    }
}

std::vector<std::string> toWords(const std::string& str) {

    std::vector<std::string> words;
    std::string temp;
    for (char ch : str) {
        if (isLetter(ch))
            temp.push_back(ch);
        else {
            if (!temp.empty()) {
                words.push_back(temp);
                temp.clear();
            }
        }
    }
    if (!temp.empty())
        words.push_back(temp);

    if (words.empty())
        throw std::runtime_error("Line is empty");

    return words;
}

bool compWords(const std::string& a, const std::string& b) {
    int cnt_a = CountUpper(a);
    int cnt_b = CountUpper(b);
    if (cnt_a != cnt_b)
        return cnt_a < cnt_b;
    return a < b;
}

bool compIf(char n){
    if (isLetter(n))
        return 1;
    return 0;
}

int main() {

    using std::string;
    using std::vector;
    using std::cout;
    using std::cin;

    SetConsoleCP(1251);
    SetConsoleOutputCP(1251);

    try {
        string line;
        std::getline(cin, line);

        if (line.empty())
            throw std::runtime_error("Line is empty");
        auto idx = find_if(line.begin(), line.end(), compIf);
        if (idx == line.end())
            throw std::runtime_error("Line is empty");

        ChangeRegistr(line);

        cout << "\nREPLACED! (changing upper case to lower case and vice versa)\n\n";
        cout << line << '\n';

        vector<string> words = toWords(line);

        std::sort(words.begin(), words.end(), compWords);

        cout << "\nREPLACED! (sorting by the counts of uppercase letters)\n\n";
        for (const auto& w : words)
            cout << w << ' ';

        cout << '\n';
    }
    catch (const std::exception& err) {
        cout << "error - " << err.what() << "\n";
    }

    return 0;
}
