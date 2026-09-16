#include <iostream>
#include <list>
#include <string>
#include <fstream>
#include <exception>

struct Avtor {
    std::string familiya;
    std::string imya;
    std::string otchestvo;
};

struct Kniga {
    int udk = 0;
    std::string nazvanie;
    int god = 0;
    std::list<Avtor> avtory;
};

static std::string Trim(std::string s) {
    while (!s.empty() && (s.front() == ' ' || s.front() == '\t' || s.front() == '\r' || s.front() == '\n')) {
        s.erase(s.begin());
    }

    while (!s.empty() && (s.back() == ' ' || s.back() == '\t' || s.back() == '\r' || s.back() == '\n')) {
        s.pop_back();
    }

    return s;
}

static bool AvtorMenshe(const Avtor& a, const Avtor& b) {

    if (a.familiya != b.familiya) 
        return a.familiya < b.familiya;
    
    if (a.imya != b.imya) 
        return a.imya < b.imya;
    
    return a.otchestvo < b.otchestvo;
}

static bool AvtorRaven(const Avtor& a, const Avtor& b) {
    return a.familiya == b.familiya && a.imya == b.imya && a.otchestvo == b.otchestvo;
}

static void VstavitAvtoraOtsort(std::list<Avtor>& spisok, Avtor a) {

    a.familiya = Trim(a.familiya);
    a.imya = Trim(a.imya);
    a.otchestvo = Trim(a.otchestvo);

    if (a.familiya.empty() || a.imya.empty() || a.otchestvo.empty()) 
        return;
    

    for (const auto& x : spisok) 
        if (AvtorRaven(x, a)) 
            return;
        
    

    auto it = spisok.begin();

    while (it != spisok.end() && !AvtorMenshe(a, *it)) 
        it++;
    
    spisok.insert(it, a);
}

static void VstavitKniguOtsort(std::list<Kniga>& biblioteka, Kniga k) {

    k.nazvanie = Trim(k.nazvanie);
    auto it = biblioteka.begin();

    while (it != biblioteka.end() && it->nazvanie < k.nazvanie) 
        it++;

    biblioteka.insert(it, k);
}

static bool UdalitKnigu(std::list<Kniga>& biblioteka, const std::string& nazvanie) {

    std::string t = Trim(nazvanie);

    for (auto it = biblioteka.begin(); it != biblioteka.end(); it++) 
        if (it->nazvanie == t) {
            biblioteka.erase(it);
            return true;
        }
    

    return false;
}

static const Kniga* NaytiKniguPoNazvaniyu(const std::list<Kniga>& biblioteka, const std::string& nazvanie) {

    std::string t = Trim(nazvanie);

    for (const auto& k : biblioteka) 
        if (k.nazvanie == t) 
            return &k;
        
    return nullptr;
}

static void NaytiKnigiPoAvtoru(const std::list<Kniga>& biblioteka, const std::string& familiya) {

    std::string f = Trim(familiya);
    bool found = false;

    for (const auto& k : biblioteka) 
        for (const auto& a : k.avtory) 
            if (a.familiya == f) {
                std::cout << "  " << k.nazvanie << " (" << k.god << "), UDK=" << k.udk << "\n";
                found = true;
                break;
            }
        
    

    if (!found) 
        std::cout << "  not founded\n";
    
}

static Kniga* NaytiKniguTochno(std::list<Kniga>& biblioteka, const std::string& nazvanie) {
    std::string t = Trim(nazvanie);

    for (auto& k : biblioteka) 
        if (k.nazvanie == t) 
            return &k;
        
    return nullptr;
}

static bool DobavitAvtoraVKnygu(std::list<Kniga>& biblioteka, const std::string& nazvanie, const Avtor& a) {

    Kniga* k = NaytiKniguTochno(biblioteka, nazvanie);
    if (!k)
        return false;

    VstavitAvtoraOtsort(k->avtory, a);
    return true;
}

static bool UdalitAvtoraIzKnigi(std::list<Kniga>& biblioteka, const std::string& nazvanie, const std::string& familiya) {

    Kniga* k = NaytiKniguTochno(biblioteka, nazvanie);
    if (!k)
        return false;

    std::string f = Trim(familiya);

    for (auto it = k->avtory.begin(); it != k->avtory.end(); it++)
        if (it->familiya == f) {
            k->avtory.erase(it);
            return true;
        }
    
    return false;
}

static std::string NextField(const std::string& s, size_t& pos, char delim) {

    std::string out;

    while (pos < s.size() && s[pos] != delim) {
        out.push_back(s[pos]);
        ++pos;
    }

    if (pos < s.size() && s[pos] == delim) 
        ++pos;
    
    return Trim(out);
}

static void LoadFromFile(std::list<Kniga>& biblioteka, const std::string& filename) {

    std::ifstream fin(filename);

    if (!fin)
        throw std::runtime_error("not founded file: " + filename);

    std::string line;

    while (std::getline(fin, line)) {

        line = Trim(line);

        if (line.empty())
        continue;
        
        size_t pos = 0;

        std::string s_udk = NextField(line, pos, '|');
        std::string s_name = NextField(line, pos, '|');
        std::string s_year = NextField(line, pos, '|');
        std::string s_auth = NextField(line, pos, '|');

        Kniga k;

        try {
            k.udk = std::stoi(s_udk);
            k.nazvanie = s_name;
            k.god = std::stoi(s_year);
        } 
        catch (...) {
            continue;
        }

        size_t p2 = 0;

        while (p2 < s_auth.size()) {

            std::string one = NextField(s_auth, p2, ';');
            if (one.empty()) 
                continue;

            size_t p3 = 0;
            Avtor a;

            a.familiya = NextField(one, p3, ',');
            a.imya = NextField(one, p3, ',');
            a.otchestvo = NextField(one, p3, ',');

            VstavitAvtoraOtsort(k.avtory, a);
        }

        if (!k.nazvanie.empty())
            VstavitKniguOtsort(biblioteka, k);
    }
}

static void PrintAll(const std::list<Kniga>& biblioteka) {

    if (biblioteka.empty()) {
        std::cout << "empty\n";
        return;
    }

    for (const auto& k : biblioteka) {

        std::cout << "- " << k.nazvanie << " (" << k.god << "), UDK=" << k.udk << "\n";
        std::cout << "  avtors: ";

        if (k.avtory.empty()) {
            std::cout << "нет\n";
            continue;
        }

        bool perviy = true;

        for (const auto& a : k.avtory) {
            if (!perviy) 
                std::cout << "; ";
            perviy = false;
            std::cout << a.familiya << " " << a.imya << " " << a.otchestvo;
        }
        std::cout << "\n";
    }
}

int main() {

    std::list<Kniga> biblioteka;

    try {
        LoadFromFile(biblioteka, "library.txt");
    } 
    catch (const std::exception& e) {
        std::cout << "ERROR: " << e.what() << "\n";
        return 1;
    }

    std::cout << "=== after upload file ===\n";
    PrintAll(biblioteka);

    std::cout << "\n=== Add book ===\n";

    Kniga nova;
    nova.udk = 100;
    nova.nazvanie = "How STL works";
    nova.god = 2025;

    VstavitAvtoraOtsort(nova.avtory, {"Ivanov", "Ivan", "Ivanovich"});
    VstavitAvtoraOtsort(nova.avtory, {"Petrov", "Petr", "Petrovich"});

    VstavitKniguOtsort(biblioteka, nova);
    PrintAll(biblioteka);

    std::cout << "\n=== Search with title ===\n";

    const Kniga* k = NaytiKniguPoNazvaniyu(biblioteka, "C++");

    if (!k)
        std::cout << "  not founded\n";
    else 
        std::cout << "  " << k->nazvanie << " (" << k->god << "), UDK=" << k->udk << "\n";
    

    std::cout << "\n=== Search books of this avtor (Famalia: 'Knut') ===\n";
    NaytiKnigiPoAvtoru(biblioteka, "Knut");

    std::cout << "\n=== Add avtor to 'How STL works' ===\n";

    if (!DobavitAvtoraVKnygu(biblioteka, "How STL works", {"Sidorov", "Sidor", "Sidorovich"})) 
        std::cout << "  book is not founded\n";

    PrintAll(biblioteka);

    std::cout << "\n=== Delete (Ivanov) from book 'How STL works' ===\n";

    if (!UdalitAvtoraIzKnigi(biblioteka, "How STL works", "Ivanov")) 
        std::cout << "  not hapenned\n";
    
    PrintAll(biblioteka);

    std::cout << "\n=== Delete book 'How STL works' ===\n";

    if (!UdalitKnigu(biblioteka, "How STL works")) 
        std::cout << "  book is not founded\n";
    

    PrintAll(biblioteka);

    return 0;
}
