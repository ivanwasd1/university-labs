#include <iostream>
#include <set>
#include <exception>

int main() {

    try {
        
        long long N;
        if (!(std::cin >> N) || N < 2)
            throw std::runtime_error("You type N wrong!!!");
        std::set<int> resheto;
        for (int i = 2; i <= N; i++)
            resheto.insert(i);

        for (int i = 2; i * i <= N; i++) {
            if (resheto.find(i) == resheto.end()) 
                continue;
            for (int j = i * i; j <= N; j += i) 
                resheto.erase(j);
        }
        
        for (int el : resheto) 
            std::cout << el << ' ';
        
    }
    catch (const std::exception& error) {
        std::cout << "ERROR " << error.what() << '\n';
    }
    return 0;
}