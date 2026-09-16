#include <iostream>

/*
С клавиатуры вводится натуральное десятичное число.
Написать программу, которая конвертирует данное число
в двоичную и шестнадцатеричную системы счисления.
*/

void write_bin_num (int t) {

    if (t == 0)
        return;

    write_bin_num(t / 2);
    std::cout << t % 2;

}

void write_hex_num (int t) {

    if (t == 0)
        return;

    write_hex_num(t / 16);
    if (t % 16 <= 9)
        std::cout << t % 16;
    else
        std::cout << char('A' + t % 16 - 10);
}

int main()
{
    using std::cin;
    using std::cout;
    int n;
    cout << "enter the n: ";
    if(!(cin >> n) || n < 1) {
        cout << "ERROR!!!";
        std::exit(1);
    }
    cout << "bin number is ";
    write_bin_num(n);
    cout << '\n';
    cout << "hex number is ";
    write_hex_num(n);

    return 0;
}
