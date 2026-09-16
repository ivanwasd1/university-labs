#include <algorithm>
#include <cmath>
#include <iostream>
#include <limits>
#include <numeric>
#include <exception>
#include <string>
#include <vector>

static void PrintVector(const std::vector<int>& values, const std::string& title)
{
    std::cout << "\n" << title << " (size=" << values.size() << "): ";

    for (std::size_t i = 0; i < values.size(); ++i)
    {
        std::cout << values[i];
        if (i + 1 != values.size())
            std::cout << ' ';
    }

    std::cout << '\n';
}

static std::vector<int> ReadVectorFromStdin()
{
    std::cout
        << "Vvedite celye chisla dlya vector.\n"
        << "Okonchanie vvoda - lyuboy NE-chislovoy simvol (naprimer: q).\n"
        << "Primer: 1 2 0 -5 7 q\n\n";

    std::vector<int> values;
    int x = 0;

    while (std::cin >> x)
        values.push_back(x);

    if (std::cin.eof())
        throw std::runtime_error("Vvod zavershilsya EOF. A nuzhno eshe vvesti parametry (n, l, r).");

    std::cin.clear();
    std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');

    if (values.empty())
        throw std::runtime_error("Vector pust. Nuzhno vvesti hotya by odno chislo.");

    return values;
}

static int ReadInt(const std::string& prompt)
{
    while (true)
    {
        std::cout << prompt;

        int x = 0;
        if (std::cin >> x)
            return x;

        std::cout << "Oshibka: nuzhno celoe chislo. Povtori.\n";

        std::cin.clear();
        std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
    }
}

static std::size_t ReadIndex(const std::string& prompt, std::size_t max_index)
{
    while (true)
    {
        std::cout << prompt;

        long long x = -1;
        if ((std::cin >> x) && (x >= 0) && (static_cast<std::size_t>(x) <= max_index))
            return static_cast<std::size_t>(x);

        std::cout << "Oshibka: index dolzhen byt v [0.." << max_index << "]. Povtori.\n";

        std::cin.clear();
        std::cin.ignore(std::numeric_limits<std::streamsize>::max(), '\n');
    }
}

int main()
{
    try
    {
        std::vector<int> values = ReadVectorFromStdin();
        PrintVector(values, "Ishodny vector");

        const long long sum = std::accumulate(values.begin(), values.end(), 0LL);
        std::cout << "\n1) Summa chisel: " << sum << '\n';

        std::cout << "2) Obshchee kolichestvo chisel: " << values.size() << '\n';

        const int target = ReadInt("\nVvedite chislo dlya podscheta ravnyh emu elementov: ");
        const std::size_t equal_count = static_cast<std::size_t>(
            std::count(values.begin(), values.end(), target)
        );
        std::cout << "3) Kolichestvo ravnyh " << target << ": " << equal_count << '\n';

        const int n = ReadInt("\nVvedite n (uslovie: chislo > n): ");
        std::size_t greater_count = 0;

        for (std::size_t i = 0; i < values.size(); ++i)
            if (values[i] > n)
                greater_count++;

        std::cout << "4) Kolichestvo chisel > " << n << ": " << greater_count << '\n';

        const int mean_int = static_cast<int>(sum / static_cast<long long>(values.size()));

        for (std::size_t i = 0; i < values.size(); ++i)
            if (values[i] == 0)
                values[i] = mean_int;

        std::cout << "\n5) Zamenil nuli srednim (celaya chast): " << mean_int << '\n';
        PrintVector(values, "Posle zameny nuley");

        const std::size_t max_index = values.size() - 1U;
        std::size_t left = ReadIndex("\nVvedite index l: ", max_index);
        std::size_t right = ReadIndex("Vvedite index r: ", max_index);

        if (left > right)
            std::swap(left, right);

        long long interval_sum = 0;

        for (std::size_t i = left; i <= right; ++i)
            interval_sum += values[i];

        for (std::size_t i = 0; i < values.size(); ++i)
            values[i] = static_cast<int>(static_cast<long long>(values[i]) + interval_sum);

        std::cout << "\n6) Dobavil k kazhdomu elementu summu intervala [l..r] = "
                  << interval_sum << '\n';
        PrintVector(values, "Posle dobavleniya");

        const int min_value = *std::min_element(values.begin(), values.end());
        const int max_value = *std::max_element(values.begin(), values.end());
        const int diff = max_value - min_value;

        for (std::size_t i = 0; i < values.size(); ++i)
        {
            const long long abs_value = std::llabs(static_cast<long long>(values[i]));
            if ((abs_value % 2) == 0)
                values[i] = diff;
        }

        std::cout << "\n7) Zamenil chisla s chetnym |x| na (max-min) = " << diff << '\n';
        PrintVector(values, "Posle zameny po |x|");

        std::vector<int> unique_by_abs;
        unique_by_abs.reserve(values.size());

        for (std::size_t i = 0; i < values.size(); ++i)
        {
            const long long abs_value = std::llabs(static_cast<long long>(values[i]));
            bool seen = false;

            for (std::size_t j = 0; j < unique_by_abs.size(); ++j)
            {
                const long long abs_seen = std::llabs(static_cast<long long>(unique_by_abs[j]));

                if (abs_seen == abs_value)
                {
                    seen = true;
                    break;
                }
            }

            if (!seen)
                unique_by_abs.push_back(values[i]);
        }

        values.swap(unique_by_abs);

        std::cout << "\n8) Udalil povtory po modulyu (ostavil pervoe vhozhdenie).\n";
        PrintVector(values, "Itogovy vector");
    }
    catch (const std::exception& e)
    {
        std::cout << "ERROR: " << e.what() << '\n';
        return 1;
    }

    return 0;
}
