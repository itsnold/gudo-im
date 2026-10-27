"""Make a separate practice copy without changing the submitted SQL."""
from hashlib import sha256
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'sources' / '4-311Group_FiveNightsAtGudoW2.sql'
EXPECTED_HASH = 'e7a44ac687fac8c6da1fd2c989caa6345639c2f5eab3d7078dbea42873f5f245'


def build_practice(source):
    if sha256(source).hexdigest() != EXPECTED_HASH:
        raise ValueError('The original SQL changed. Review it before making a new practice copy.')
    text = source.decode('utf-8')
    edits = [
        (
            'DROP DATABASE IF EXISTS campus_equipment_borrowing;\nCREATE DATABASE campus_equipment_borrowing',
            'CREATE DATABASE gudo_im_practice',
        ),
        ('USE campus_equipment_borrowing;', 'USE gudo_im_practice;'),
        (
            'CONSTRAINT fk_transaction_processor FOREIGN KEY (processed_by)\n'
            '        REFERENCES USER_ACCOUNT (user_id)\n'
            '        ON UPDATE CASCADE ON DELETE RESTRICT,',
            'CONSTRAINT fk_transaction_processor FOREIGN KEY (processed_by)\n'
            '        REFERENCES USER_ACCOUNT (user_id),',
        ),
    ]
    for before, after in edits:
        if text.count(before) != 1:
            raise ValueError('An expected edit no longer matches the original SQL.')
        text = text.replace(before, after, 1)
    header = (
        '-- Practice copy of the submitted W2 SQL.\n'
        '-- Creates gudo_im_practice. It does not drop or reset a database.\n'
        '-- Stop if that database already exists; use a fresh practice instance.\n'
        '-- Processor FK uses default restrictive actions to avoid the MySQL CHECK conflict.\n'
        '-- The seed data and other table constraints are unchanged.\n'
    )
    return header + text


if __name__ == '__main__':
    output = ROOT / 'practice.sql'
    output.write_text(build_practice(SOURCE.read_bytes()), encoding='utf-8')
    print('Created practice.sql. The submitted SQL is unchanged.')
