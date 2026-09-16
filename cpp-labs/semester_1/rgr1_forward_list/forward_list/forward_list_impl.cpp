#include "forward_list_impl.h"

ForwardList::ForwardList() : head_(nullptr), size_(0) {}

ForwardList::~ForwardList() {
    Clear();
}

void ForwardList::PushFront(int32_t value) {
    Node* n = new Node(value);
    n->next_ = head_;
    head_ = n;
    ++size_;
}

void ForwardList::PopFront() {
    if (!head_) return;
    Node* old = head_;
    head_ = head_->next_;
    delete old;
    --size_;
}

size_t ForwardList::Size() const {
    return size_;
}

void ForwardList::Clear() {
    Node* cur = head_;
    while (cur) {
        Node* next = cur->next_;
        delete cur;
        cur = next;
    }
    head_ = nullptr;
    size_ = 0;
}

int32_t ForwardList::Front() const {
    if (!head_) 
        throw std::out_of_range("list is emtry");
    return head_->value_;
}

bool ForwardList::FindByValue(int32_t value) const {

    for (Node* cur = head_; cur; cur = cur->next_) 
        if (cur -> value_ == value) 
            return true;
    
    return false;
}

void ForwardList::Print(std::ostream& out) const {
    Node* cur = head_;
    bool first = true;
    while (cur) {
        if (!first) out << ' ';
        first = false;
        out << cur->value_;
        cur = cur->next_;
    }
}

ForwardList::ForwardList(size_t count, int32_t value) : head_(nullptr), size_(0) {
    Node* tail = nullptr;
    for (size_t i = 0; i < count; i++) {
        Node* n = new Node(value);
        if (!head_) {
            head_ = n;
            tail = n;
        } else {
            tail->next_ = n;
            tail = n;
        }
        ++size_;
    }
}

ForwardList::ForwardList(const ForwardList& rhs) : head_(nullptr), size_(0) {

    if (!rhs.head_) 
        return;

    head_ = new Node(rhs.head_ -> value_);
    Node* tail = head_;
    ++size_;

    for (Node* cur = rhs.head_->next_; cur; cur = cur->next_) {
        tail->next_ = new Node(cur -> value_);
        tail = tail->next_;
        ++size_;
    }
}

ForwardList::ForwardList(std::initializer_list<int32_t> init) : ForwardList() {
    for (auto it = init.end(); it != init.begin();) {
        --it;
        PushFront(*it);
    }
}

ForwardList& ForwardList::operator=(const ForwardList& neww) {

    if (this == &neww)
        return *this;
    ForwardList tmp(neww);
    Swap(tmp);
    return *this;
}

void ForwardList::Remove(int32_t value) {
    while (head_ && head_->value_ == value) {
        Node* old = head_;
        head_ = head_->next_;
        delete old;
        --size_;
    }

    Node* prev = head_;
    if (!prev) return;

    Node* runner = head_->next_;
    while (runner) {
        if (runner->value_ == value) {
            prev->next_ = runner->next_;
            delete runner;
            runner = prev->next_;
            --size_;
        } else {
            prev = runner;
            runner = runner->next_;
        }
    }
}
