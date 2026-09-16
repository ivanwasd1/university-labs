#pragma once


// iterator interface
template <typename T>
class Iterator {
public:
    virtual void First() = 0;
    virtual void Next() = 0;
    virtual const T& CurrentItem() const = 0;
    virtual bool IsDone() const = 0;

    virtual ~Iterator() = default;
};
